# Sistema inteligente para monitoramento de umidade em fraldas infantis por sensoriamento capacitivo e acesso remoto

Trabalho de Conclusão de Curso de **Helen Borges de Lima Souza**, Engenharia de Controle e Automação, Universidade Federal de Ouro Preto (UFOP).

O MOMO é um **protótipo acadêmico em desenvolvimento**. O projeto investiga se a variação de capacitância medida em uma fralda pode indicar o início de um evento e distinguir urina, fezes e diarreia nos ensaios de bancada. O sistema proposto combina sensoriamento com o FDC1004, aquisição pelo XIAO ESP32-C6, classificação de séries temporais e acompanhamento em um aplicativo Android. A avaliação disponível é experimental e exploratória, sem validação clínica ou indicação de uso infantil.

## Estrutura

| Pasta | Conteúdo |
| --- | --- |
| [aquisicao/](aquisicao/) | Sketches Arduino de caracterização e resposta temporal, registros CSV, gráficos SVG e anotações ambientais. |
| [algoritmos/](algoritmos/) | Código Python da investigação por MMQ e da avaliação de Random Forest + 1-NN com DTW. |
| [firmware/](firmware/) | Estado da entrega do firmware de operação e do processamento embarcado. |
| [aplicativo/momo/](aplicativo/momo/) | Projeto Android nativo em Kotlin e Jetpack Compose, com configuração Gradle e testes do aplicativo. |
| [docs/](docs/) | Seleção de arquivos, verificações e publicação no GitHub. |

## Aquisição experimental

O FDC1004 mede a capacitância no canal 0. Os sketches configuram o conversor para 100 Hz e programam leituras seriais a cada 250 ms. A comunicação I²C usa SDA no GPIO 22 (D4) e SCL no GPIO 23 (D5) no XIAO ESP32-C6. Ambos registram tempo em milissegundos, capacitância em pF, CAPDAC, status e marcador. O sketch temporal aceita uma única marca de aplicação e encerra 120 s depois, com opção de parada manual. Consulte o procedimento e o significado dos campos. [Instruções de aquisição](aquisicao/README.md)

Os 18 CSVs fornecidos incluem três ensaios temporais e três ensaios na série de umidade por material. Os gráficos e as anotações de temperatura e umidade ambiente foram preservados ao lado dos respectivos CSVs quando fornecidos. O ZIP de entrada original foi preservado para que os dois scripts possam abri-lo diretamente. Os registros mantêm os mesmos bytes do anexo.

## Métodos avaliados

- **MMQ.** Usa os nove ensaios temporais. Calcula a diferença entre capacitância média após a aplicação e a linha de base seca, a tendência linear e o intervalo interquartil após a aplicação. Padroniza as características com os dados de treino de cada rodada e ajusta por mínimos quadrados. A avaliação é leave-one-out por ensaio.
- **Random Forest + 1-NN com DTW.** Usa 15 ensaios com um marcador de aplicação. A Random Forest binária usa desvio em relação à referência seca e diferenças de primeira e segunda ordem. O segmento começa na primeira previsão positiva após a calibração de 30 s. O 1-NN compara o prefixo desse segmento com segmentos de referência dos ensaios de treino pela distância DTW.
- **Diferença em relação ao protocolo descrito.** O código recebido seleciona o comprimento do prefixo no treino interno de cada rodada, equilibrando acurácia balanceada e antecipação. A descrição do método final estabelece 21 leituras fixas. O script desta entrega reproduz a variante de avaliação fornecida, com o comprimento selecionado registrado em `k_train`.

Amostras do mesmo ensaio ficam juntas nas divisões de treino e teste. O MMQ usa a marca real de aplicação para calcular suas características e utiliza o trecho posterior completo. A avaliação de RF + DTW usa essa marca para gerar os rótulos de treino e verificar se o alerta saiu antes da aplicação. Veja os comandos de reprodução e os limites de interpretação. [Instruções das análises](algoritmos/README.md)

## Firmware e aplicativo

Os dois sketches fornecidos implementam aquisição experimental por serial. O firmware de operação que executaria o classificador no XIAO ESP32-C6 e publicaria a telemetria, assim como modelos treinados e exportações embarcadas, está pendente de fornecimento. [Estado do firmware](firmware/README.md)

O MOMO contém um modo de demonstração com dados simulados e um modo conectado. No modo conectado, usa Firebase Authentication com login Google e Firebase Realtime Database para telemetria, vínculos e histórico. O provisionamento inicial usa HTTP local pelo ponto de acesso temporário do dispositivo. A reprodução integrada depende de firmware compatível e da configuração de um projeto Firebase próprio. [Instruções do aplicativo](aplicativo/momo/README.md)

## Limitações

O conjunto contém poucas repetições por classe e as avaliações usam apenas os registros fornecidos. A base de RF + DTW mistura ensaios temporais e ensaios de umidade de fezes e diarreia, enquanto a classe urina entra apenas com a série temporal. Isso limita a interpretação da comparação entre classes e a generalização para outros protocolos ou condições de medição. O repositório permite identificar e reproduzir a variante fornecida, mas a equivalência com o protocolo de 21 leituras e a operação integrada do protótipo dependem dos artefatos finais da pesquisa.

[Inventário e verificação](docs/inventario.md) · [Publicação no GitHub](docs/publicacao.md)
