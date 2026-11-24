# Aquisição experimental

## Sketches e conexão

1. No Arduino IDE, instale o pacote de placas `esp32` da Espressif e a **ProtoCentral FDC1004 Capacitive Sensor Library**, que fornece `Protocentral_FDC1004.h`. `Wire.h` faz parte do core. [Biblioteca ProtoCentral](https://github.com/protocentral/ProtoCentral_fdc1004_breakout)
2. Selecione `XIAO_ESP32C6` e a porta do dispositivo. Ligue o FDC1004 pelo I²C com SDA em D4/GPIO 22 e SCL em D5/GPIO 23 e use o canal 0. Abra o arquivo `.ino` dentro da pasta de mesmo nome e carregue o sketch desejado.
3. Abra a serial a 115200 baud. Envie `I` para iniciar, `A` para marcar a aplicação e `F` para terminar. O [sketch de caracterização](sketches/Ensaios_de_caracterizacao/Ensaios_de_caracterizacao.ino) admite múltiplas aplicações. O [sketch temporal](sketches/Ensaios_de_resposta_temporal/Ensaios_de_resposta_temporal.ino) registra uma aplicação e encerra automaticamente 120 s depois.
4. Para um novo ensaio, capture em arquivo separado a saída da porta serial. Os sketches emitem mensagens de controle iniciadas por `#` junto com as linhas de leitura. Os cabeçalhos nos CSVs anexados estão em inglês. O material fornecido não inclui o procedimento intermediário de captura/exportação desses CSVs.

| Campo entregue | Significado no protocolo do sketch |
| --- | --- |
| `Time(ms)` | Tempo desde o início do ensaio. |
| `Capacitance(pF)` | Capacitância registrada, em pF. Em caso de erro o sketch pode emitir o último valor válido ou zero se ainda não houver leitura válida. |
| `CAPDAC` | Valor do CAPDAC registrado pelo sketch. |
| `Status` | 0 leitura válida, 1 CAPDAC fora da faixa ou em ajuste, −1 erro ou sensor indisponível. |
| `Marker` | 0 registro sem evento, 1 aplicação, 2 início, 3 fim. |

## Organização dos registros

Em [dados/](dados/), o diretório expandido replica a organização do ZIP recebido.

| Grupo | CSVs | Arquivos complementares |
| --- | --- | --- |
| `urina_temporal`, `fezes_temporal`, `diarreia_temporal` | 3 por classe, 1 marcador de aplicação por CSV | 1 gráfico SVG e 1 anotação ambiental TXT por CSV |
| `umidade_fezes`, `umidade_diarreia` | 3 por classe, 1 marcador de aplicação por CSV | 1 gráfico SVG e 1 anotação ambiental TXT por CSV |
| `umidade_urina` | 3, 4 marcadores de aplicação por CSV | Apenas os CSVs no anexo |

O repositório preserva ordem, conteúdo e finais de linha. Os gráficos são artefatos de visualização exportados pelo Qt e os TXT registram temperatura e umidade relativa. O [ZIP original](dados/ensaios%285%29.zip) é a entrada direta dos scripts. Mantenha as cópias expandidas para consulta e use o ZIP para reprodução. Volumes, composição das amostras e demais condições de bancada devem ser consultados no texto do TCC quando disponíveis.
