# Deslize pra Limpar

A forma mais rápida de organizar sua galeria e liberar espaço no Android.

[![Google Play](https://img.shields.io/badge/Google%20Play-Install-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.lagando.deslize)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.13-green?style=for-the-badge)](https://play.google.com/store/apps/details?id=com.lagando.deslize)

<p align="center">
  <img src="logo2.png" alt="Deslize pra Limpar" width="200"/>
</p>

## Sobre

O **Deslize pra Limpar** é um aplicativo Android leve e eficiente para organizar sua galeria de fotos e vídeos. Com um simples gesto de deslizar, você pode remover arquivos indesejados e liberar espaço no seu dispositivo.

- **Tamanho**: Apenas ~4MB — um dos apps de galeria mais leves disponíveis
- **Modo offline**: Funciona 100% offline — nenhuma foto ou vídeo é enviada para a internet
- **Gratuito**: Sem custos, sem assinaturas
- **Sem anúncios**: Experiência limpa e focada

## Capturas de Tela

> **TODO**: Adicionar screenshots reais do app aqui.
>
> Recomendação: 3-4 prints mostrando a tela de swipe, a lixeira, a tela de "tudo limpo" e o menu lateral.

## Funcionalidades

| Recurso | Descrição |
|---------|-----------|
| **Deslizar para excluir** | Deslize para a esquerda para remover fotos/vídeos |
| **Deslizar para manter** | Deslize para a direita para arquivar/manter o arquivo |
| **Decidir depois** | Pule itens que você quer rever mais tarde |
| **Modos de exibição** | Aleatório, sequencial, por tamanho ou data |
| **Desfazer** | Desfaça o último swipe com um toque |
| **Fotos mantidas** | Galeria com grid das fotos que você decidiu manter |
| **Lixeira** | Área segura para arquivos removidos antes da exclusão permanente |
| **Limpeza automática** | Coleta periódica de itens expirados na lixeira (30 dias) |
| **Contador de espaço** | Veja quanto espaço será liberado e quanto já foi liberado |
| **Compartilhar** | Compartilhe mídias diretamente do app |
| **Multi-seleção** | Selecione múltiplos itens nas fotos mantidas para ações em lote |

## Download

[Disponível na Google Play](https://play.google.com/store/apps/details?id=com.lagando.deslize)

## Privacidade

- ✅ **Funciona offline** — Nenhuma mídia é enviada ou carregada para servidores externos
- ✅ **Sem coleta de dados** — Sua privacidade é garantida
- ✅ **Sem anúncios** — Experiência sem interrupções

> O app possui permissão de internet apenas para exibir avisos opcionais do desenvolvedor no menu lateral. Nenhuma foto, vídeo ou dado pessoal é transmitido.

## Informações Técnicas

| Detalhe | Valor |
|---------|-------|
| **Versão** | 1.0.13 (debug: 1.0.13-debug) |
| **SDK Mínimo** | Android 6.0 (API 23) |
| **SDK Alvo** | Android 15 (API 37) |
| **Idiomas** | Português (PT-BR) e Inglês |
| **Linguagem** | Kotlin |
| **Arquitetura** | MVC com Fragments |
| **Banco de dados** | Room |
| **Carregamento de imagens** | Glide 5 |
| **Tarefas em segundo plano** | WorkManager |
| **UI** | Material Design 3 (dark theme) |

---

## 🛠️ Compilação e Desenvolvimento

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

3. **Configure o ambiente:**
   - O projeto usa `compileSdk 37`, `minSdk 23`, `targetSdk 37`
   - As dependências estão no `android/app/build.gradle`
   - Certifique-se de ter o Android SDK instalado

4. **Execute o aplicativo:**
   - Conecte um dispositivo Android via USB ou use um emulador
   - Clique no botão "Run" (▶️) no Android Studio
   - Ou use o comando:
     ```bash
     cd android && ./gradlew installDebug
     ```

### Estrutura do Código

```
android/app/src/main/
├── AndroidManifest.xml                    # Permissões e declaração de Activities
├── java/com/lagando/deslize/
│   ├── MainActivity.kt                   # Activity principal — navegação e permissões
│   ├── SwipeFragment.kt                  # Tela principal — swipe para escolher
│   ├── KeptFragment.kt                   # Grid de fotos mantidas
│   ├── TrashFragment.kt                  # Lista de itens na lixeira
│   ├── PhotoViewerActivity.kt            # Visualizador fullscreen de mídia
│   ├── MyAppGlideModule.kt              # Configuração do Glide
│   ├── model/
│   │   └── MediaItem.kt                  # Modelo in-memory de mídia
│   ├── data/
│   │   ├── AppDatabase.kt               # Configuração do Room
│   │   ├── MediaDao.kt                   # Queries do banco de dados
│   │   └── MediaItemEntity.kt           # Entidade do Room
│   ├── adapter/
│   │   ├── KeptAdapter.kt              # RecyclerView adapter — grid de mantidas
│   │   └── TrashAdapter.kt             # RecyclerView adapter — lista da lixeira
│   └── worker/
│       └── TrashCleanupWorker.kt        # WorkManager — limpeza automática
└── res/
    ├── layout/                           # Layouts XML das telas
    ├── drawable/                         # Ícones vetoriais e backgrounds
    ├── menu/                             # Menus de navegação e toolbar
    ├── values/                           # Strings (EN), cores, temas
    └── values-pt/                        # Strings em Português (PT-BR)
```

### Dependências Principais

Definidas em `android/app/build.gradle`:

| Biblioteca | Versão | Função |
|-----------|--------|--------|
| **AndroidX Core** | 1.19.0 | Extensões Kotlin para Android |
| **Material** | 1.14.0 | Material Design 3 components |
| **Room** | 2.8.4 | Banco de dados SQLite com ORM |
| **Glide** | 5.0.9 | Carregamento e cache de imagens |
| **WorkManager** | 2.11.2 | Tarefas em segundo plano |
| **Coroutines** | 1.11.0 | Programação assíncrona |

### Configuração de Signing (Release)

> **⚠️ Importante**: Nunca commite arquivos `.keystore` nem senhas no repositório.

O projeto lê as credenciais de signing de properties do Gradle. Para configurar:

1. **Crie ou edite `~/.gradle/gradle.properties`** (arquivo global, fora do repositório):
   ```properties
   RELEASE_STORE_FILE=caminho/para/seu/release.keystore
   RELEASE_STORE_PASSWORD=sua_senha
   RELEASE_KEY_ALIAS=seu_alias
   RELEASE_KEY_PASSWORD=sua_senha
   ```

2. **Gerar um novo keystore** (se necessário):
   ```bash
   keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
   ```

3. **Build de Release:**
   ```bash
   cd android && ./gradlew assembleRelease
   ```
   O APK assinado será gerado em `android/app/build/outputs/apk/release/`

### Contribuição

Contribuições são bem-vindas! Para contribuir:

1. **Fork** o repositório no GitHub
2. Crie um branch para sua feature: `git checkout -b feature/nova-funcionalidade`
3. Commit suas mudanças: `git commit -m 'feat: adiciona nova funcionalidade'`
4. Push para o branch: `git push origin feature/nova-funcionalidade`
5. Abra um **Pull Request** descrevendo o que foi feito

### 📝 Notas para Agentes de IA

Se você está lendo este código como um agente de IA:

- O projeto usa **MVC com Fragments** (sem ViewModel atualmente)
- O banco de dados usa **Room** com DAO patterns para acesso a dados
- O carregamento de imagens usa **Glide** com módulo personalizado (`MyAppGlideModule.kt`)
- A lógica de permissão de mídia está em `MainActivity.kt` com suporte a Android 6-15
- O worker de limpeza usa **WorkManager** periódico (1 dia, limpa itens com 30+ dias na lixeira)
- O app solicita permissões: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `POST_NOTIFICATIONS`, `INTERNET`
- O design usa Material Design 3 com tema dark personalizado (`Theme.DeslizeLimpar`)
- Senhas de signing **não estão no código** — são lidas de `gradle.properties`

## Licença

Este projeto está licenciado sob a [MIT License](LICENSE).

---

Desenvolvido por [Lagando](https://lagando.com.br/) — 2026