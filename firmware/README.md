# Firmware e processamento embarcado

O [sketch integrado](momo_embarcado/momo_embarcado.ino) reúne a leitura do FDC1004, a inferência local e a comunicação do XIAO ESP32-C6 com o aplicativo. Os [sketches de aquisição experimental](../aquisicao/sketches/) continuam disponíveis para coleta independente.

## Configuração de bancada

1. Abra `momo_embarcado/momo_embarcado.ino` no Arduino IDE, selecione **XIAO ESP32-C6** e instale **ProtoCentral FDC1004 Capacitive Sensor Library** e **ArduinoJson 7**. Mantenha os cabeçalhos na pasta do sketch. Ligue SDA em D4/GPIO 22 e SCL em D5/GPIO 23.
2. Copie [momo_config.example.h](momo_embarcado/momo_config.example.h) para `momo_config.h` na mesma pasta. Preencha a senha do ponto de acesso temporário, a chave Web do Firebase, o endereço do banco e o e-mail, senha e UID da conta técnica do dispositivo. Use uma senha SoftAP de pelo menos oito caracteres. O Git ignora o arquivo preenchido.
3. Em um projeto Firebase de teste, configure autenticação por e-mail e senha para o dispositivo e login Google para o aplicativo. Revise as [regras fornecidas](../aplicativo/firebase/firebase_realtime_rules_prototipo.json) antes de aplicá-las. O vínculo relaciona o UID do cuidador ao UID técnico. As regras do protótipo foram fornecidas para demonstração acadêmica e não verificam a posse física do dispositivo.
4. Compile, grave e abra o Monitor Serial a 115200 baud. No telefone, conecte-se ao SoftAP temporário, envie pelo MOMO a configuração da rede Wi-Fi de 2,4 GHz e volte à internet para verificar o estado do dispositivo. Comece a calibração com a fralda seca. O ESP publica em `deviceStatus/{uid}` e consulta `deviceCommands/{uid}`.

O firmware publica o estado, a identificação e a classificação no nó de status. As confirmações de troca são registradas pelo aplicativo. O cliente Android também acompanha a coleção `users/{uid}/sensorEvents`, distinta da telemetria de estado publicada por este sketch. O [README principal](../README.md#ensaios-funcionais-do-sistema-integrado) resume as observações de bancada da monografia.

## Modelo e reprodução

O [modelo exportado](momo_embarcado/momo_modelo_treinado.json) registra **100 árvores**, **15 assinaturas**, **21 leituras** e **30 s de calibração**. A Random Forest detecta uma transição. A partir dela, o firmware acumula o prefixo de 21 desvios de capacitância e usa DTW para escolher uma das três classes. O código classifica uma vez por ciclo. O protótipo não estima saturação e carece de validação clínica.

O modelo fornecido já está incluído no cabeçalho. Para reproduzir a exportação sem sobrescrevê-lo, use um caminho de saída separado. Execute a partir da raiz, após instalar as [dependências Python](../algoritmos/requirements.txt).

```powershell
python firmware/momo_embarcado/exportar_modelo_esp32.py --ensaios "aquisicao/dados/ensaios(5).zip" --analise algoritmos/rf_dtw/Random_Forest_1NN_DTW_Helen_Borges.py --saida modelo_reproduzido.h
g++ -std=c++17 -O2 firmware/momo_embarcado/teste_replay.cpp -o teste_replay.exe
python firmware/momo_embarcado/comparar_replay.py --executavel ./teste_replay.exe --ensaios "aquisicao/dados/ensaios(5).zip" --analise algoritmos/rf_dtw/Random_Forest_1NN_DTW_Helen_Borges.py
```

Os scripts auxiliares ajustam detectores quando executados. O replay verifica concordância entre implementações nos ensaios usados para o modelo e não mede capacidade de generalização. A reprodução do treinamento e o teste físico ficaram pendentes.
