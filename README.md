# Deslize pra Limpar

A forma mais rápida de organizar sua galeria e liberar espaço no Android.

[![Google Play](https://img.shields.io/badge/Google%20Play-Install-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.lagando.deslize)

## Sobre o Deslize pra Limpar

O **Deslize pra Limpar** é um aplicativo Android leve e eficiente para organizar sua galeria de fotos e vídeos. Com um simples gesto de deslizar, você pode remover arquivos indesejados e liberar espaço no seu dispositivo.

- **Tamanho**: Apenas ~4MB - um dos apps de galeria mais leves disponíveis
- **Modo offline**: 100% offline - nenhuma foto ou vídeo é enviada para a internet
- **Gratuito**: Sem custos, sem assinaturas
- **Sem anúncios**: Experiência limpa e focada

## Funcionalidades Principais

| Recurso | Descrição |
|---------|-----------|
| **Deslizar para excluir** | Deslize para a esquerda para remover fotos/videos |
| **Deslizar para manter** | Deslize para a direita para arquivar/manter o arquivo |
| **Modos de exibição** | Aleatório, sequencial, por tamanho ou data |
| **Ordenação** | Maiores arquivos primeiro, mais recentes primeiro |
| **Lixeira** | Área segura para arquivos removidos antes da exclusão permanente |
| **Limpeza automática** | Coleta periódica de itens expirados na lixeira |
| **Contador de espaço** | Visualize quanto espaço será liberado |

## Capturas de Tela

*(Adicionar prints da interface do app aqui)*

## Download

[Disponível na Google Play](https://play.google.com/store/apps/details?id=com.lagando.deslize)

## Privacidade

- ✅ **100% offline** - Nenhuma mídia é enviada ou carregada para servidores externos
- ✅ **Sem coleta de dados** - Sua privacidade é garantida
- ✅ **Sem anúncios** - Experiência sem interrupções

## Informações Técnicas

- **Versão**: 1.0.13 (variante debug: 1.0.13-debug)
- **SDK Mínimo**: Android 23 (API Level 23 / Android 6.0)
- **SDK Alvo**: Android 37 (API Level 37 / Android 15)
- **Idiomas**: Português (PT-BR) e Inglês
- **Arquitetura**: Java/Kotlin com Room (banco de dados), Glide (imagens), WorkManager (limpeza automática)

## Licença

MIT License

Copyright (c) 2026 Lagando

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## 🛠️ Compilação e Desenvolvimento

Este projeto foi desenvolvido para Android e pode ser compilado usando **Android Studio**. Siga estas instruções para rodar ou modificar o código:

### Pré-requisitos

- [Android Studio](https://developer.android.com/studio) (versão mais recente recomendada)
- [Java Development Kit (JDK) 17](https://openjdk.java.net/projects/jdk/17/) ou superior
- [Git](https://git-scm.com/) para clonar o repositório

### Como rodar o projeto

1. **Clone o repositório:**
   ```bash
   git clone git@github.com:lagando/LeitorAFK.git
   cd LeitorAFK
   ```

2. **Abra no Android Studio:**
   - Inicie o Android Studio
   - Selecione "Open" e navegue até a pasta `android/` deste projeto
   - Ou rode diretamente: `cd android && studio .`

3. **Configure o ambiente:**
   - O projeto usa `compileSdk 37`, `minSdk 23`, `targetSdk 37`
   - As dependências estão no `android/app/build.gradle`
   - Certifique-se de ter o Android SDK instalado

4. **Execute o aplicativo:**
   - Conecte um dispositivo Android via USB ou use um emulator
   - Clique no botão "Run" (ícone de ▶️) no Android Studio
   - Ou use o comando: `./gradlew installDebug`

### Estrutura do Código

O código-fonte está organizado da seguinte forma:

```
android/app/src/main/java/com/lagando/deslize/
├── MainActivity.kt          # Activity principal - gerencia navegação e permisões
├── MyAppGlideModule.kt      # Módulo Glide - configuração de carregamento de imagens
├── PhotoViewerActivity.kt   # Activity para visualização de mídia
├── SwipeFragment.kt         # Fragment principal - tela de deslizar/escolher
├── KeptFragment.kt          # Fragment - fotos mantidas/arquivadas
├── TrashFragment.kt         # Fragment - lixeira de arquivos removidos
├── model/MediaItem.kt       # Modelo de dados - representa um item de mídia
├── data/AppDatabase.kt      # Configuração do Room banco de dados
├── data/MediaDao.kt         # Data Access Object - queries do banco
├── data/MediaItemEntity.kt  # Entidade do Room banco de dados
├── adapter/KeptAdapter.kt  # Adaptador RecyclerView - fotos mantidas
└── adapter/TrashAdapter.kt # Adaptador RecyclerView - lixeira

android/app/src/main/java/com/lagando/deslize/worker/
└── TrashCleanupWorker.kt  # Worker WorkManager - limpeza automática

android/app/src/main/java/com/lagando/deslize/
├── res/                     # Recursos (layout, strings, drawables, values)
├── AndroidManifest.xml    # Manifest do app - permissões e atividades
└── res/values-pt/strings.xml # Strings em Português
            res/values/strings.xml # Strings em Inglês (padrão)
```

### Dependências Principais

O projeto utiliza as seguintes bibliotecas chave (definidas no `android/app/build.gradle`):

- **AndroidX**: AppCompat, Lifecycle, Room, RecyclerView, WorkMaterial
- **Glide**: `com.github.bumptech.glide:glide:5.0.9` - carregamento e cache de imagens
- **WorkManager**: `androidx.work:work-runtime-ktx:2.11.2` - tarefas em segundo plano
- **Coroutines**: `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0`

### Configuração de Signing (Para Release na Play Store)

**IMPORTANTE**: Nunca commite seus arquivos `.keystore` no repositório! Eles devem permanecer local.

O projeto já tem `signingConfigs` configurado no `android/app/build.gradle`, mas os arquivos reais foram removidos do versionamento por segurança.

Para gerar um novo keystore ou usar o seu existente:

1. **Gerar um novo keystore:**
   ```bash
   keytool -genkey -v -keystore ~/minhokeystore.keystore -alias lagando -keyalg RSA -keysize 2048 -validity 10000
   ```
   *Preencha os dados solicitados (nome, organização, etc.)*

2. **Configurar no Gradle:**
   No arquivo `android/gradle.properties` ou em variáveis de ambiente, configure:
   ```properties
   storePassword=sua_senha
   keyPassword=sua_senha
   storeFile=~/caminho/para/minhokeystore.keystore
   keyAlias=lagando
   ```

   Ou configure no `android/app/build.gradle` diretamente (não recomendado para segredos):
   ```gradle
   signingConfigs {
       release {
           storeFile file('~/caminho/para/minhokeystore.keystore')
           storePassword 'sua_senha'
           keyAlias 'lagando'
           keyPassword 'sua_senha'
       }
   }
   ```

3. **Build de Release:**
   ```bash
   ./gradlew assembleRelease
   ```
   Isso gerará o APK assinado em `android/app/build/outputs/apk/release/`

### Git e Contribuição

Este é um repositório público para o projeto **Deslize pra Limpar**. Se desejar contribuir:

1. **Fork** o repositório no GitHub
2. Crie um branch para sua feature: `git checkout -b feature/nova-funcionalidade`
3. Commit suas mudanças: `git commit -m 'feat: adiciona nova funcionalidade'`
4. Push para o branch: `git push origin feature/nova-funcionalidade`
5. Abra um **Pull Request** descrevendo o que foi feito

### Estrutura de Pastas Importante

- **`android/app/src/main/`**: Código-fonte e recursos da aplicação
- **`android/app/src/main/res/values/`**: Arquivos de strings (PT e EN)
- **`android/app/src/main/res/layout/`**: Layouts XML das telas
- **`android/app/src/main/res/drawable/`**: Ícones e imagens do app
- **`android/.gitignore`**: Arquivo que exclui build artifacts e keystores do versionamento
- **`android/gradle/`**: Configurações do build system
- **`logo2.png`**: Logo do projeto (está no root do repositório)

### 📝 Notas para Agentes de IA

Se você está lendo este código como um agente de IA:

- O projeto segue arquitetura **MVVMC** (Model-View-ViewModel with Compose-inspired patterns using Fragments)
- O banco de dados usa **Room** comDao patterns para acesso a dados
- O carregamento de imagens usa **Glide** com módulo personalizado (`MyAppGlideModule.kt`)
- A lógica de permissão de mídia está em `MainActivity.kt` com suporte a Android 12/13/14/15
- O worker de limpeza usa **WorkManager** periódico (1 dia)
- O app solicita permissões de `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` e `POST_NOTIFICATIONS`
- O design segue guidelines de Material Design com tema próprio (`Theme.DeslizeLimpar`)

---
Desenvolvido por [Lagando](https://lagando.com.br/) - 2026