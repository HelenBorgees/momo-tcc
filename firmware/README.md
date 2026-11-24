# Firmware e processamento embarcado

Os arquivos Arduino disponíveis são os [sketches de aquisição experimental](../aquisicao/sketches/). Eles leem o FDC1004, gerenciam o ensaio e escrevem pela porta serial. O código Python está em [algoritmos/](../algoritmos/).

O firmware de operação do XIAO ESP32-C6, incluindo eventual inferência de Random Forest e DTW, as assinaturas fixas e a publicação de telemetria, não foi fornecido. As interfaces de configuração e telemetria esperadas pelo cliente estão documentadas no [aplicativo MOMO](../aplicativo/momo/README.md). Elas descrevem o lado Android, sem confirmar o comportamento de uma implementação embarcada.
