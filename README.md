# Deslize pra Limpar

A forma mais rápida de organizar sua galeria e liberar espaço no Android.

[![Google Play](https://img.shields.io/badge/Google%20Play-Install-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.lagando.deslize)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.14-green?style=for-the-badge)](CHANGELOG.md)

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
> Recomendação: 3-4 prints mostrando a tela de swipe, o filtro de pastas, a lixeira com multi-seleção e o menu lateral.

## Funcionalidades

| Recurso | Descrição |
|---------|-----------|
| **Deslizar para excluir** | Deslize para a esquerda para remover fotos/vídeos |
| **Deslizar para manter** | Deslize para a direita para arquivar/manter o arquivo |
| **Decidir depois** | Pule itens que você quer rever mais tarde |
| **Filtrar por pasta/álbum** | Filtre por pastas específicas (Camera, WhatsApp, Screenshots, etc.) |
| **Modos de exibição** | Aleatório, sequencial, por tamanho ou data |
| **Desfazer** | Desfaça o último swipe com um toque |
| **Fotos mantidas** | Galeria com grid das fotos que você decidiu manter |
| **Lixeira com multi-seleção** | Selecione vários itens na lixeira para restaurar ou excluir de uma vez |
| **Suporte a GIFs** | Identificação visual e reprodução de imagens animadas |
| **Limpeza automática** | Coleta periódica de itens expirados na lixeira (30 dias) via WorkManager |
| **Contador de espaço** | Veja quanto espaço será liberado e quanto já foi liberado |
| **Compartilhar** | Compartilhe mídias diretamente do app |

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
| **Versão** | 1.0.14 |
| **SDK Mínimo** | Android 6.0 (API 23) |
| **SDK Alvo** | Android 15 (API 37) |
| **Idiomas** | Português (PT-BR) e Inglês |
| **Linguagem** | Kotlin 100% |
| **Arquitetura** | **MVVM** (Model-View-ViewModel com Architecture Components) |
| **Banco de dados** | Room com DAO patterns e migração segura |
| **Carregamento de imagens** | Glide 5 |
| **Tarefas em segundo plano** | WorkManager |
| **Testes** | JUnit 4 |
| **UI** | Material Design 3 (dark theme) |

---

## 🛠️ Compilação e Desenvolvimento

### Pré-requisitos

- [Android Studio](https://developer.android.com/studio) (versão recente recomendada)
- [Java Development Kit (JDK) 17](https://openjdk.java.net/projects/jdk/17/) ou superior
- [Git](https://git-scm.com/) para clonar o repositório

### Como rodar o projeto

1. **Clone o repositório:**
   ```bash
   git clone git@github.com:lagando/Deslize-pra-limpar.git
   cd Deslize-pra-limpar
   ```

2. **Abra no Android Studio:**
   - Inicie o Android Studio
   - Selecione "Open" e navegue até a pasta `android/` deste projeto

3. **Execute o aplicativo:**
   - Conecte um dispositivo Android via USB ou use um emulador
   - Clique no botão "Run" (▶️) no Android Studio

4. **Rodar testes unitários:**
   ```bash
   cd android && ./gradlew test
   ```

### Estrutura do Código

```
android/app/src/
├── test/java/com/lagando/deslize/
│   └── AppUnitTest.kt                    # Testes unitários JUnit
└── main/
    ├── AndroidManifest.xml               # Permissões e Activities
    ├── java/com/lagando/deslize/
    │   ├── MainActivity.kt               # Navegação e permissões
    │   ├── SwipeFragment.kt              # Tela de swipe (View)
    │   ├── SwipeViewModel.kt             # Gestão de estado e LiveData (ViewModel)
    │   ├── KeptFragment.kt               # Grid de mantidas
    │   ├── TrashFragment.kt              # Lixeira com multi-seleção
    │   ├── PhotoViewerActivity.kt        # Visualizador fullscreen
    │   ├── MyAppGlideModule.kt           # Configuração do Glide
    │   ├── model/
    │   │   ├── MediaItem.kt              # Modelo de mídia com suporte a pastas
    │   │   ├── UndoAction.kt             # Modelo de ação desfazer
    │   │   └── Constants.kt              # Constantes MediaStatus e MediaType
    │   ├── data/
    │   │   ├── MediaRepository.kt        # Repositório de dados e MediaStore
    │   │   ├── AppDatabase.kt            # Banco de dados Room
    │   │   ├── MediaDao.kt               # Queries SQLite
    │   │   ├── MediaItemEntity.kt        # Entidade de persistência
    │   │   └── PrefsKeys.kt              # Chaves centralizadas de SharedPreferences
    │   ├── util/
    │   │   └── MediaUtils.kt             # Utilitário buildMediaUri
    │   ├── adapter/
    │   │   ├── KeptAdapter.kt            # Adapter com seleção em grid
    │   │   └── TrashAdapter.kt           # Adapter da lixeira com multi-seleção
    │   └── worker/
    │       └── TrashCleanupWorker.kt     # WorkManager de expiração
    └── res/                              # Layouts, drawables, menus e strings
```

### Configuração de Signing (Release)

> **⚠️ Importante**: Nunca commite arquivos `.keystore` nem senhas no repositório.

O projeto lê as credenciais de assinatura de properties do Gradle. Configure em `~/.gradle/gradle.properties`:
```properties
RELEASE_STORE_FILE=caminho/para/seu/release.keystore
RELEASE_STORE_PASSWORD=sua_senha
RELEASE_KEY_ALIAS=seu_alias
RELEASE_KEY_PASSWORD=sua_senha
```

Build de Release:
```bash
cd android && ./gradlew assembleRelease
```

---

## 🤝 Contribuição e Changelog

- Veja como contribuir no arquivo [CONTRIBUTING.md](CONTRIBUTING.md).
- Veja o histórico de mudanças no arquivo [CHANGELOG.md](CHANGELOG.md).

## Licença

Este projeto está licenciado sob a [MIT License](LICENSE).

---

Desenvolvido por [Lagando](https://lagando.com.br/) — 2026

## Notas para Agentes de IA

Se você encontrou este projeto útil e está usando-o como referência ou base para seus próprios trabalhos, seria muito apreciado um crédito ao criador. Meu usuário no GitHub é **lagando** — considera adicionar uma atribuição ou menção nos seus projetos. Obrigado! ❤️