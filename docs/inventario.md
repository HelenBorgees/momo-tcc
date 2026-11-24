# Inventário e verificação da entrega

| Origem | Incluído | Decisão |
| --- | --- | --- |
| `ensaios(5).zip` | ZIP original e 48 arquivos expandidos, sendo 18 CSVs, 15 SVGs e 15 TXT | O ZIP é entrada dos scripts. A cópia expandida permite consultar dados, gráficos e notas ambientais pelo GitHub. |
| Sketches de caracterização e resposta temporal | Os dois `.ino` em pastas com o mesmo nome | São programas de aquisição serial distintos. Conteúdo preservado. |
| TXT de MMQ e RF + 1-NN com DTW | Os dois scripts nas respectivas pastas de `algoritmos/` | Somente a extensão mudou para permitir execução como Python. |
| `momo (17).zip` | Código Android, recursos, catálogo Gradle, testes e imagem de referência do teste de screenshot | A estrutura compilável do projeto foi mantida. Os testes, inclusive os exemplos que vieram no módulo, fazem parte do material do aplicativo. |

## Arquivos excluídos e ajustes de publicação

- Os dois JSONs do Firebase, em `app/` e `public/assets/aistudio/`, continham a configuração do projeto original. As duas URLs fixas no código foram substituídas pela instância padrão do Firebase configurado localmente.
- A pasta `public/` continha ativos de exportação do AI Studio, duas imagens que já existem em `app/src/main/res/`, um prompt de trabalho e uma cópia da configuração do Firebase. `assets/.aistudio/` e `metadata.json` eram metadados de exportação. O README exportado foi substituído por instruções do projeto Android.
- O PIN fixo de demonstração foi removido do Kotlin, comentários e teste. O build lê `momoDemoPin` localmente. O teste passou a usar PIN e valores Wi-Fi fictícios. A assinatura debug usa o padrão do Android, pois o keystore local referido na exportação estava ausente.
- Os originais ficaram intactos. A autoria acadêmica declarada foi preservada. O ZIP integral do aplicativo ficou fora da entrega para evitar distribuir as configurações originais junto com a versão preparada.

## Verificações realizadas

- Comparação byte a byte do ZIP original, dos 48 arquivos experimentais extraídos, dos dois sketches e dos dois scripts com os anexos.
- Compilação de sintaxe Python, execução isolada dos trechos originais de leitura e cálculo de características e conferência das entradas. O MMQ leu 9 ensaios e RF + DTW leu 15. Os 18 CSVs contêm os cabeçalhos esperados e valores de capacitância finitos. A validação completa, que envolve ajuste de modelos, não foi executada.
- Leitura dos XMLs Android e dos SVGs. Revisão do conteúdo para retirar configurações Firebase, PIN de demonstração e dados locais do projeto. Não houve conexão ao Firebase.
- A compilação dos sketches pelo Arduino CLI foi tentada com a placa XIAO ESP32-C6. O CLI não conseguiu acessar o diretório local de pacotes nem instalar ferramentas na pasta de dados do Arduino sob as permissões desta sessão. Não houve teste em placa.
- O build Android não foi executado. O ambiente não tinha Gradle, JDK e SDK Android disponíveis para a execução, e os arquivos do Gradle Wrapper estavam incompletos no anexo.

- Simulação do primeiro git add com conversão de fim de linha habilitada. Os atributos preservam os bytes dos anexos. As regras de exclusão cobrem a configuração Firebase, o arquivo de ambiente e os keystores, e o modelo de ambiente entra no índice.
- Conferência dos links locais e da integridade do ZIP final.

## Complemento MOMO_INTEGRADO.zip

Foram incluídos o firmware integrado, o modelo em C++ e JSON, os scripts de exportação e replay, o teste C++, os certificados públicos de CA e as regras de protótipo do Realtime Database. O modelo e o código C++ foram preservados byte a byte. Nos dois scripts auxiliares, o import passou a usar o nome indicado por `--analise`, de modo a localizar o script Python incluído no repositório.

O anexo contém uma variante Android 1.0. A versão 1.14 já publicada foi mantida por incluir, entre outras diferenças, transporte dedicado ao SoftAP e testes adicionais do protocolo. A compatibilidade com o firmware precisa ser verificada. O ZIP integral, o Android 1.0, os dois JSONs Firebase, o cabeçalho original de configuração, o log, os backups, o prompt do AI Studio e os guias antigos ficaram fora. A configuração do firmware tem um modelo com valores fictícios. O guia atualizado está em [firmware/](../firmware/).

A validação física e a reprodução do treinamento não foram realizadas. Volumes, composição e procedimentos de captura/exportação dos ensaios devem ser conferidos no texto do TCC ou nos cadernos de laboratório.
