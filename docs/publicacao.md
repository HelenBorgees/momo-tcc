# Publicar no GitHub

1. Extraia o ZIP e abra um terminal na pasta `momo-tcc`, onde está este repositório.
2. Crie um repositório vazio na sua conta GitHub e escolha a visibilidade desejada. Deixe as opções de inicialização com README, licença e gitignore desmarcadas. Autentique o Git antes do envio. [Guia do GitHub](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)
3. Inicialize o histórico e examine os arquivos preparados para o primeiro commit:

   ```powershell
   git init -b main
   git add .
   git status --short
   git diff --cached --stat
   ```

4. Depois de conferir a lista, faça o primeiro commit e o envio. Substitua `SEU_USUARIO` e `SEU_REPOSITORIO` pelos valores do repositório criado.

   ```powershell
   git commit -m "Organiza materiais do TCC MOMO"
   git remote add origin https://github.com/SEU_USUARIO/SEU_REPOSITORIO.git
   git push -u origin main
   ```

5. Abra o repositório no GitHub, confira se o README e os links aparecem e use a URL do commit citado no apêndice para identificar a versão dos materiais. A configuração Firebase e as chaves de assinatura devem ficar locais.
