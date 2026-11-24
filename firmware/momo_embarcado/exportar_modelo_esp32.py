#!/usr/bin/env python3
"""Exporta RF+DTW para C++ usando exclusivamente dados e algoritmo do Colab."""

import argparse
import importlib
import json
import sys
from pathlib import Path

import numpy as np


def literal_float(valor):
    texto = format(float(valor), ".9g")
    if "." not in texto and "e" not in texto:
        texto += ".0"
    return texto + "f"


def gerar_header(saida, trials, detector, segmentos, k, hm):
    # Ordem lexicografica reproduz desempates de min((dist, nome_classe)).
    classes = ("diarreia", "fezes", "urina")
    arvores = detector.estimators_
    offsets = []
    total = 0
    for arv in arvores:
        offsets.append(total)
        total += arv.tree_.node_count
    if total >= 65535:
        raise RuntimeError("Indices das arvores nao cabem em uint16_t")

    with saida.open("w", encoding="utf-8", newline="\n") as fd:
        fd.write("// Gerado automaticamente a partir dos 15 registros. NAO EDITAR.\n")
        fd.write("#pragma once\n#include <stdint.h>\n")
        fd.write("#ifndef PROGMEM\n#define PROGMEM\n#endif\n")
        fd.write("namespace momo_modelo {\n")
        fd.write("struct NoRF {\n  double limiar;\n  float p_evento;\n")
        fd.write("  uint16_t esquerdo, direito;\n  int8_t atributo;\n};\n")
        fd.write(f"static constexpr uint16_t TOTAL_NOS = {total};\n")
        fd.write(f"static constexpr uint8_t TOTAL_ARVORES = {len(arvores)};\n")
        fd.write(f"static constexpr uint8_t K_PREFIXO = {k};\n")
        fd.write(f"static constexpr uint8_t TOTAL_TEMPLATES = {len(trials)};\n")
        fd.write("static constexpr uint32_t CALIBRACAO_MS = 30000UL;\n")
        fd.write("static const uint16_t RAIZES[TOTAL_ARVORES] PROGMEM = {\n  ")
        fd.write(", ".join(str(x) for x in offsets) + "\n};\n")
        fd.write("static const NoRF NOS[TOTAL_NOS] PROGMEM = {\n")
        for deslocamento, estimator in zip(offsets, arvores):
            arv = estimator.tree_
            if not np.array_equal(estimator.classes_, [0, 1]):
                raise RuntimeError("Ordem de classes inesperada no RF")
            for no in range(arv.node_count):
                feature = int(arv.feature[no])
                if feature < 0:
                    esq = dir_ = 65535
                else:
                    esq = deslocamento + int(arv.children_left[no])
                    dir_ = deslocamento + int(arv.children_right[no])
                contagens = arv.value[no][0]
                prob = float(contagens[1] / contagens.sum())
                fd.write("  {" + format(float(arv.threshold[no]), ".17g") + ", "
                         + literal_float(prob) + f", {esq}, {dir_}, {feature}" + "},\n")
        fd.write("};\n")
        fd.write("// Classes: 0=diarreia, 1=fezes, 2=urina.\n")
        fd.write("static const uint8_t CLASSE_TEMPLATE[TOTAL_TEMPLATES] PROGMEM = {\n  ")
        fd.write(", ".join(str(classes.index(t.label)) for t in trials) + "\n};\n")
        fd.write("static const double ASSINATURAS[TOTAL_TEMPLATES][K_PREFIXO] PROGMEM = {\n")
        for i in range(len(trials)):
            prefixo = segmentos[i][1][:k]
            fd.write("  {" + ", ".join(format(float(v), ".17g") for v in prefixo) + "},\n")
        fd.write("};\n}  // namespace momo_modelo\n")

    return {"arvores": len(arvores), "nos": total, "k": k,
            "hm_interno": hm, "templates": len(trials),
            "calibracao_ms": 30000, "classes": classes,
            "ensaios": [t.name for t in trials],
            "nota": "Treinado com 15 registros completos de aplicacao unica. "
                    "A validacao aninhada 13/15 NAO representa teste externo "
                    "do modelo final ajustado sobre todos os registros."}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--ensaios", type=Path, required=True)
    parser.add_argument("--analise", type=Path, required=True)
    parser.add_argument("--saida", type=Path, default=Path(__file__).with_name("momo_modelo_treinado.h"))
    args = parser.parse_args()

    sys.path.insert(0, str(args.analise.parent.resolve()))
    rf_dtw = importlib.import_module(args.analise.stem)

    ensaios = rf_dtw.load_trials(args.ensaios)
    if len(ensaios) != 15:
        raise RuntimeError("Este exportador foi preparado para os 15 ensaios de aplicacao unica")
    sequencias = [rf_dtw.causal_features(t, 30000.0) for t in ensaios]
    ids = list(range(len(ensaios)))
    segmentos = {}
    for i in ids:
        ids_internos = [j for j in ids if j != i]
        detector_sem_i = rf_dtw.make_detector(ids_internos, sequencias)
        segmentos[i] = rf_dtw.segment(detector_sem_i, sequencias[i], 30000.0)
        if segmentos[i] is None:
            raise RuntimeError(f"Deteccao nao encontrada: {ensaios[i].name}")
        print(f"Assinatura {i+1}/{len(ensaios)}: {ensaios[i].name}", flush=True)

    k, hm = rf_dtw.choose_decision_prefix(segmentos, ids, ensaios)
    detector_final = rf_dtw.make_detector(ids, sequencias)
    info = gerar_header(args.saida, ensaios, detector_final, segmentos, k, hm)
    args.saida.with_suffix(".json").write_text(
        json.dumps(info, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in info.items() if k != "ensaios"},
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
