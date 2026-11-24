AUTORA = "Helen Borges de Lima Souza"
METODO = "Random Forest + 1-NN com DTW"

import csv
import io
import json
import zipfile
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from sklearn.ensemble import RandomForestClassifier


CLASSES = ("urina", "fezes", "diarreia")


@dataclass
class Trial:
    name: str
    label: str
    time: np.ndarray
    capacitance: np.ndarray
    marker: np.ndarray

    @property
    def event_ms(self):
        events = self.time[self.marker == 1]
        if len(events) != 1:
            raise ValueError(f"{self.name}: esperado um único marcador; vieram {len(events)}")
        return float(events[0])


def load_trials(archive_path):
    trials = []
    with zipfile.ZipFile(archive_path) as archive:
        for member in sorted(archive.namelist()):
            if not member.endswith(".csv"):
                continue
            if Path(member).name.startswith("urina_ensaio"):
                continue
            parts = Path(member).parts
            labels = [c for c in CLASSES if any(c in p for p in parts[:-1])]
            if len(labels) != 1:
                raise ValueError(f"Rótulo não identificado para {member}")
            with archive.open(member) as fd:
                rows = list(csv.DictReader(io.TextIOWrapper(fd, encoding="utf-8-sig")))
            get = lambda key: np.array([float(x[key]) for x in rows])
            trial = Trial(Path(member).name, labels[0], get("Time(ms)"),
                          get("Capacitance(pF)"), get("Marker").astype(int))
            if len(trial.time) < 10 or not np.all(np.diff(trial.time) > 0):
                raise ValueError(f"Tempos inválidos em {member}")
            if not np.isfinite(trial.capacitance).all():
                raise ValueError(f"Capacitância não finita em {member}")
            if not trial.event_ms > 0:
                raise ValueError(f"Marcador inválido em {member}")
            trials.append(trial)
    return trials


def causal_features(trial, calibration_ms):


    keep = trial.marker == 0
    t = trial.time[keep]
    c = trial.capacitance[keep]
    dry = c[t < calibration_ms] if calibration_ms > 0 else c[:1]
    if len(dry) == 0:
        raise ValueError("Calibração sem amostras")
    if calibration_ms and trial.event_ms <= calibration_ms:
        raise ValueError(f"{trial.name}: aplicação durante calibração")
    c0 = float(dry.mean())
    first = np.diff(c, prepend=c[0])
    second = np.diff(first, prepend=first[0])
    X = np.column_stack([c - c0, first, second])
    training_labels = (t > trial.event_ms).astype(int)
    return t, X, training_labels


def make_detector(trial_indices, seqs):
    X = np.concatenate([seqs[i][1] for i in trial_indices])
    y = np.concatenate([seqs[i][2] for i in trial_indices])
    model = RandomForestClassifier(random_state=0,
                                   class_weight="balanced_subsample", n_jobs=-1)
    return model.fit(X, y)


def segment(detector, seq, calibration_ms):


    t, X, unused_labels = seq
    can_predict = (t >= calibration_ms) if calibration_ms else (np.arange(len(t)) > 0)
    predictions = detector.predict(X)
    triggers = np.flatnonzero(can_predict & (predictions == 1))
    if len(triggers) == 0:
        return None
    k = int(triggers[0])
    return t[k:], X[k:, 0]


def dtw_diagonal_prefix(x, y, n):


    previous = np.full(n + 1, np.inf)
    previous[0] = 0.0
    diagonal = np.empty(n)
    for i in range(1, n + 1):
        current = np.full(n + 1, np.inf)
        xi = x[i - 1]
        for j in range(1, n + 1):
            diff = xi - y[j - 1]
            current[j] = diff * diff + min(previous[j],
                                           current[j - 1], previous[j - 1])
        diagonal[i - 1] = current[i] / i
        previous = current
    return diagonal


def choose_decision_prefix(segments, training_ids, trials):


    shortest = min(len(segments[i][1]) for i in training_ids)
    classes_by_trial = np.array([trials[i].label for i in training_ids])
    prediction = np.empty((len(training_ids), shortest), dtype=object)
    for row, i in enumerate(training_ids):
        candidates = [
            (dtw_diagonal_prefix(segments[i][1], segments[j][1], shortest),
             trials[j].label)
            for j in training_ids if i != j
        ]
        for k in range(shortest):
            prediction[row, k] = min((dist[k], material)
                                     for dist, material in candidates)[1]

    best_key = (-np.inf, -np.inf, -np.inf)
    best_k = None
    for k in range(1, shortest + 1):
        recalls = [np.mean(prediction[classes_by_trial == material, k - 1] == material)
                   for material in CLASSES]
        balanced_acc = float(np.mean(recalls))
        earliness = float(np.mean([k / len(segments[i][1])
                                  for i in training_ids]))
        if balanced_acc + 1 - earliness == 0:
            hm = 0
        else:
            hm = 2 * balanced_acc * (1 - earliness) / (
                 balanced_acc + (1 - earliness))
        key = (hm, balanced_acc, -k)
        if key > best_key:
            best_key, best_k = key, k
    return best_k, float(best_key[0])


def online_decision(test_segment, train_segments, train_ids, trials, k):

    if test_segment is None or len(test_segment[1]) < k:
        return None
    candidates = []
    for i in train_ids:
        if len(train_segments[i][1]) < k:
            continue
        d = dtw_diagonal_prefix(test_segment[1], train_segments[i][1], k)[-1]
        candidates.append((d, trials[i].label))
    if not candidates:
        return None
    return {"ms": float(test_segment[0][k - 1]),
            "predicted": min(candidates)[1]}


def run_cv(trials, calibration_s=30.0):
    calibration_ms = 1000 * calibration_s
    seqs = [causal_features(t, calibration_ms) for t in trials]
    results = []
    for test_i, test in enumerate(trials):
        training_ids = [i for i in range(len(trials)) if i != test_i]
        outer_detector = make_detector(training_ids, seqs)
        segments = {}
        for i in training_ids:
            inner_train = [j for j in training_ids if j != i]
            inner_detector = make_detector(inner_train, seqs)
            segments[i] = segment(inner_detector, seqs[i], calibration_ms)
        if any(segments[i] is None for i in training_ids):
            raise ValueError("Detecção ausente no treino interno; " +
                             "necessário protocolo de seleção com rejeição")
        k, hm_train = choose_decision_prefix(segments, training_ids, trials)
        test_segment = segment(outer_detector, seqs[test_i], calibration_ms)
        alarm = online_decision(test_segment, segments, training_ids, trials, k)
        after_marker = alarm is not None and alarm["ms"] >= test.event_ms
        pred = alarm["predicted"] if after_marker else None
        results.append({
            "trial": test.name, "real": test.label, "k_train": k,
            "train_hm": round(hm_train, 5), "marker_ms": test.event_ms,
            "onset_ms": float(test_segment[0][0]) if test_segment is not None else None,
            "alert_ms": alarm["ms"] if alarm else None,
            "predicted": pred, "detected": bool(after_marker),
            "correct": bool(after_marker and pred == test.label),
            "false_positive": bool(alarm and not after_marker),
            "delay_s": round((alarm["ms"] - test.event_ms) / 1000, 5)
                       if after_marker else None,
            "premature_class": alarm["predicted"] if alarm and not after_marker else None,
        })
        print(f"[{len(results)}/{len(trials)}] {test.name}: "
              f"k={k}, real={test.label}, previsto={pred}, "
              f"atraso={results[-1]['delay_s']} s", flush=True)
    good = [r for r in results if r["detected"]]
    delays = [r["delay_s"] for r in good]
    confusion = {
        real: {
            predicted: sum(r["detected"] and r["real"] == real and
                           r["predicted"] == predicted for r in results)
            for predicted in CLASSES
        }
        for real in CLASSES
    }
    return {
        "method": "RF binary onset + 1-NN DTW prefix; nested leave-one-trial-out",
        "calibration_s": calibration_s,
        "n_trials": len(trials),
        "detected": len(good), "missed": len(results) - len(good),
        "correct": sum(r["correct"] for r in results),
        "false_positive": sum(r["false_positive"] for r in results),
        "median_delay_s": float(np.median(delays)) if delays else None,
        "max_delay_s": float(max(delays)) if delays else None,
        "confusion": confusion, "trials": results,
    }


def escolher_arquivo_zip():
    import sys
    informado = next((a for a in sys.argv[1:] if a.lower().endswith(".zip") and Path(a).is_file()), None)
    if informado:
        return Path(informado)
    encontrados = sorted(Path.cwd().glob("*.zip"))
    if len(encontrados) == 1:
        return encontrados[0]
    try:
        from google.colab import files
    except ImportError as exc:
        raise FileNotFoundError("Coloque um único ZIP na pasta ou informe seu caminho como argumento.") from exc
    enviados = files.upload()
    if not enviados:
        raise RuntimeError("Nenhum arquivo ZIP foi enviado.")
    return Path(next(iter(enviados)))


if __name__ == "__main__":
    print(f"Autora: {AUTORA}")
    print(f"Método: {METODO}")
    arquivo = escolher_arquivo_zip()
    ensaios = load_trials(arquivo)
    resultado = run_cv(ensaios, calibration_s=30.0)
    print(json.dumps(resultado, ensure_ascii=False, indent=2))
