# Reprodução das análises

Na raiz do repositório, com Python 3, instale as bibliotecas em um ambiente virtual. O arquivo de dependências lista os pacotes importados diretamente. As versões históricas usadas nos ensaios não foram fornecidas.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r algoritmos/requirements.txt
.\.venv\Scripts\python.exe algoritmos/mmq/MMQ_Helen_Borges.py "aquisicao/dados/ensaios(5).zip"
.\.venv\Scripts\python.exe algoritmos/rf_dtw/Random_Forest_1NN_DTW_Helen_Borges.py "aquisicao/dados/ensaios(5).zip"
```

Em Linux ou macOS, use `.venv/bin/python` nos três últimos comandos. O MMQ mostra a matriz de confusão em uma janela e imprime as características e os resultados. A execução de RF + DTW treina detectores durante a validação e imprime o progresso e o resumo JSON na saída padrão. O código também prevê upload pelo Google Colab quando não recebe um ZIP explícito nem encontra um único ZIP no diretório de execução.

## Seleção de entradas e referências

| Script | Seleção efetiva do código |
| --- | --- |
| MMQ | Os 9 CSVs dos diretórios `*_temporal`, com três por classe. |
| RF + DTW | Os 9 temporais e os 6 de umidade de fezes e diarreia. O leitor pula pelo nome os três `urina_ensaio*.csv`, que contêm múltiplas aplicações. |

A variante de RF inicia a segmentação na primeira previsão positiva elegível. Para as características da RF ela usa somente linhas com `Marker == 0`; o script não filtra por `Status`. Os segmentos de referência são reconstruídos a partir das séries de treino dentro da validação, com detecção interna por exclusão de um ensaio. A DTW acumula diferenças quadráticas e normaliza o custo pelo comprimento do prefixo. A busca interna seleciona `k_train` para cada rodada externa. A variante com 21 leituras fixas e uma eventual coleção de assinaturas usada pelo firmware não constam na entrega.

Os scripts foram copiados byte a byte dos TXT recebidos com extensão `.py`. Nenhum ajuste científico foi feito. Rodadas completas podem gerar novos resultados e são opcionais para consultar o repositório.
