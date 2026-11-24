# Aplicativo MOMO

Projeto Android nativo em Kotlin e Jetpack Compose. O módulo `app` inclui interface, recursos, dados de demonstração e testes de funcionalidades. O código usa Firebase Authentication, Realtime Database, Credential Manager, Retrofit, OkHttp, Moshi e corrotinas. O catálogo [libs.versions.toml](gradle/libs.versions.toml) mantém as versões fornecidas.

## Compilar e executar

1. Abra esta pasta no Android Studio e instale o SDK Android 36.1 exigido pelo projeto. Use JDK 17 e Gradle 9.3.1, conforme a combinação do AGP 9.1.1. Configure uma instalação do Gradle 9.3.1 no Android Studio ou no terminal. [Compatibilidade do AGP](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
2. Registre `com.helen.momo` em um projeto Firebase próprio, configure Google como provedor do Authentication e cadastre o SHA-1 da assinatura usada no teste. Crie a instância padrão do Realtime Database antes de baixar a configuração atualizada. Salve-a como `app/google-services.json`. O código usa `default_web_client_id` gerado pelo plugin Google Services e usa a URL padrão do banco configurado. O JSON deve conter as informações do cliente OAuth Web e a URL do banco. [Login Google](https://firebase.google.com/docs/auth/android/google-signin) · [Configuração do Google Services](https://firebase.google.com/docs/android/google-services-plugin-and-file)
3. Para acessar a demonstração, configure `momoDemoPin` com quatro dígitos escolhidos localmente em `~/.gradle/gradle.properties`. Também é possível passar `-PmomoDemoPin=4825` ao Gradle, usando um PIN fictício para teste. Sem esse valor, as ferramentas protegidas pelo PIN ficam bloqueadas. O PIN é incorporado ao build e serve apenas para acesso à demonstração.
4. Sincronize o Gradle e execute a variante `debug` em emulador ou dispositivo com Android API 24 ou superior. A assinatura de depuração padrão do Android substitui a referência ao arquivo local ausente. No terminal, dentro desta pasta, os comandos equivalentes de compilação e testes são:

   ```powershell
   gradle :app:assembleDebug
   gradle :app:testDebugUnitTest
   ```

5. Na tela inicial, use **Acessar Modo Demonstração (Sem Login)** para dados simulados. A integração real exige login Google e configuração do [firmware](../../firmware/) e das [regras do Realtime Database](../firebase/firebase_realtime_rules_prototipo.json) em um projeto de teste. Para release, a configuração mantida lê `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD` do ambiente e usa o alias `upload`.

## Integração do protótipo

- **Configuração local.** O celular se conecta ao SoftAP temporário do dispositivo e envia `POST /wifi-setup` a `192.168.4.1` com SSID, senha da rede escolhida, identificador e UID do proprietário. Esse endereço de configuração usa HTTP local, conforme o manifesto e a política de rede.
- **Operação remota.** O cliente observa `deviceLinks/{uid}`, `deviceStatus/{uid}` e os registros `users/{uid}/sensorEvents` e `users/{uid}/confirmedChanges`. Publica comandos em `deviceCommands/{uid}` e acompanha o identificador de confirmação. O perfil fica em `users/{uid}/babyProfile`.
- **Escopo da reprodução.** O firmware fornecido publica o status atual e responde a comandos. O aplicativo oferece um modo de demonstração com dados simulados. O [README principal](../../README.md#ensaios-funcionais-do-sistema-integrado) descreve os testes do sistema integrado registrados na monografia. Para o teste conectado, use dados fictícios e regras de acesso vinculadas à identidade autenticada.

O `.env.example` é um modelo local para o plugin de configuração. O código recebido mantém a dependência Firebase AI no Gradle, sem chamada no código-fonte analisado. Mantenha a configuração Firebase e as chaves de assinatura na sua instalação local.
