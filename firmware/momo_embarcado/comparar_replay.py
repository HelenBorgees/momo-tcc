"""Testa a mesma inferencia em Python e no executavel C++ de PC.

Nao e validacao independente: o detector final foi treinado com os 15 ensaios.
Esse teste verifica apenas concordancia de implementacao (paridade).
"""

import argparse
import importlib
import subprocess
import sys
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--executavel", type=Path, required=True)
    parser.add_argument("--ensaios", type=Path, required=True)
    parser.add_argument("--analise", type=Path, required=True)
    args = parser.parse_args()
    sys.path.insert(0, str(args.analise.parent.resolve()))
    m = importlib.import_module(args.analise.stem)

    ensaios = m.load_trials(args.ensaios)
    sequencias = [m.causal_features(t, 30000) for t in ensaios]
    ids = list(range(len(ensaios)))
    detector = m.make_detector(ids, sequencias)
    segmentos = {}
    for i in ids:
        detector_sem_i = m.make_detector([j for j in ids if j != i], sequencias)
        segmentos[i] = m.segment(detector_sem_i, sequencias[i], 30000)
    k, hm = m.choose_decision_prefix(segmentos, ids, ensaios)
    print(f"K do treino={k}, HM={hm:.6f}")

    diferencas = 0
    for i, trial in enumerate(ensaios):
        seg = m.segment(detector, sequencias[i], 30000)
        referencia = m.online_decision(seg, segmentos, ids, ensaios, k)
        # Nenhum marcador e transmitido ao C++.
        entrada = "".join(f"{int(t)},{float(c):.17g}\n"
                          for t, c, marker in zip(trial.time, trial.capacitance, trial.marker)
                          if marker == 0)
        proc = subprocess.run([str(args.executavel.resolve())], input=entrada,
                              capture_output=True, text=True, check=True)
        respostas = [linha for linha in proc.stdout.splitlines() if linha]
        previsto = None
        if respostas:
            instante, rotulo = respostas[0].split(",")
            previsto = (float(instante), rotulo)
        esperado = ((referencia["ms"], referencia["predicted"])
                    if referencia else None)
        igual = previsto == esperado and len(respostas) <= 1
        diferencas += not igual
        print(f"{trial.name}: Python={esperado}, C++={previsto}, igual={igual}")
    if diferencas:
        raise SystemExit(f"{diferencas} diferencas detectadas")
    print(f"PARIDADE CONFIRMADA: {len(ensaios)}/{len(ensaios)} reproducoes concordaram.")


if __name__ == "__main__":
    main()
