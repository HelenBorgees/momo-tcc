AUTORA = "Helen Borges de Lima Souza"
METODO = "Classificação multiclasse por mínimos quadrados (MMQ)"

import io
import re
import sys
import zipfile
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from sklearn.metrics import ConfusionMatrixDisplay, confusion_matrix
from sklearn.model_selection import LeaveOneOut
from sklearn.preprocessing import StandardScaler


CLASSES = ["Urina", "Fezes", "Diarreia"]
COLUNAS = ["ΔC médio (pF)", "Tendência (pF/s)", "IQR (pF)"]


def escolher_arquivo_zip():
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


def carregar_caracteristicas(caminho):
    registros = []
    with zipfile.ZipFile(caminho) as arquivo:
        for nome in sorted(arquivo.namelist()):
            if "_temporal/" not in nome or not nome.lower().endswith(".csv"):
                continue
            material = Path(nome).parent.name.split("_")[0].capitalize()
            if material not in CLASSES:
                continue
            numero = re.search(r"(\d+)-t\.csv$", Path(nome).name, flags=re.IGNORECASE)
            if numero is None:
                raise ValueError(f"Número do ensaio não identificado: {nome}")
            dados = pd.read_csv(io.BytesIO(arquivo.read(nome))).sort_values("Time(ms)")
            marca = dados.loc[dados["Marker"] == 1, "Time(ms)"]
            if len(marca) != 1:
                raise ValueError(f"Esperado um marcador em {nome}; encontrados {len(marca)}")
            evento = float(marca.iloc[0])
            seca = dados.loc[dados["Time(ms)"] < evento, "Capacitance(pF)"].to_numpy(dtype=float)
            depois = dados.loc[dados["Time(ms)"] > evento, ["Time(ms)", "Capacitance(pF)"]]
            tempo = (depois["Time(ms)"].to_numpy(dtype=float) - evento) / 1000
            capacitancia = depois["Capacitance(pF)"].to_numpy(dtype=float)
            if len(seca) == 0 or len(tempo) < 5 or np.any(np.diff(tempo) <= 0):
                raise ValueError(f"Leituras insuficientes ou tempos inválidos em {nome}")
            c0 = float(np.mean(seca))
            media = float(np.mean(capacitancia) - c0)
            inclinacao = float(np.polyfit(tempo, capacitancia, 1)[0])
            q25, q75 = np.percentile(capacitancia, [25, 75])
            registros.append({
                "Material": material,
                "Ensaio": int(numero.group(1)),
                "ΔC médio (pF)": media,
                "Tendência (pF/s)": inclinacao,
                "IQR (pF)": float(q75 - q25),
            })
    dados = pd.DataFrame(registros)
    if len(dados) != 9 or dados.groupby("Material").size().to_dict() != {c: 3 for c in CLASSES}:
        raise ValueError("São necessários os 9 ensaios temporais: 3 de cada classe.")
    dados["Material"] = pd.Categorical(dados["Material"], categories=CLASSES, ordered=True)
    return dados.sort_values(["Material", "Ensaio"]).reset_index(drop=True)


def validar_mmq(caracteristicas):
    X = caracteristicas[COLUNAS].to_numpy(dtype=float)
    y = caracteristicas["Material"].astype(str).to_numpy()
    if not np.isfinite(X).all():
        raise ValueError("Características inválidas ou não finitas.")
    resultados = []
    for rodada, (treino, teste) in enumerate(LeaveOneOut().split(X), start=1):
        escala = StandardScaler()
        X_treino = escala.fit_transform(X[treino])
        X_teste = escala.transform(X[teste])
        A_treino = np.column_stack([np.ones(len(treino)), X_treino])
        A_teste = np.column_stack([np.ones(len(teste)), X_teste])
        T_treino = np.array([[int(classe == nome) for nome in CLASSES] for classe in y[treino]], dtype=float)
        W, _, posto, _ = np.linalg.lstsq(A_treino, T_treino, rcond=None)
        pontuacoes = (A_teste @ W)[0]
        prevista = CLASSES[int(np.argmax(pontuacoes))]
        real = y[teste][0]
        resultados.append({
            "Rodada": rodada,
            "Material": real,
            "Ensaio": int(caracteristicas.iloc[teste[0]]["Ensaio"]),
            "Classe real": real,
            "Classe prevista": prevista,
            "Acerto": real == prevista,
            "Pontuação urina": float(pontuacoes[0]),
            "Pontuação fezes": float(pontuacoes[1]),
            "Pontuação diarreia": float(pontuacoes[2]),
            "Posto da matriz": int(posto),
        })
    return pd.DataFrame(resultados)


def exibir_resultados(caracteristicas, avaliacao):
    print("\nCaracterísticas experimentais")
    print(caracteristicas.to_string(index=False))
    print("\nValidação Leave-One-Out")
    print(avaliacao.to_string(index=False))
    acertos = int(avaliacao["Acerto"].sum())
    total = len(avaliacao)
    print(f"\nAcertos: {acertos}/{total}")
    print(f"Erros: {total-acertos}")
    print(f"Acurácia exploratória: {100 * acertos/total:.2f}%")
    matriz = confusion_matrix(avaliacao["Classe real"], avaliacao["Classe prevista"], labels=CLASSES)
    fig, ax = plt.subplots(figsize=(7, 6))
    ConfusionMatrixDisplay(confusion_matrix=matriz, display_labels=CLASSES).plot(ax=ax, cmap="Blues", values_format="d", colorbar=True)
    ax.set_title("Matriz de Confusão — MMQ (Leave-One-Out)")
    ax.set_xlabel("Classe prevista")
    ax.set_ylabel("Classe real")
    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    print(f"Autora: {AUTORA}")
    print(f"Método: {METODO}")
    arquivo = escolher_arquivo_zip()
    caracteristicas = carregar_caracteristicas(arquivo)
    avaliacao = validar_mmq(caracteristicas)
    exibir_resultados(caracteristicas, avaliacao)
