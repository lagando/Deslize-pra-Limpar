# Changelog

Todas as alterações notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.14] - 2026-10-25

### ✨ Adicionado
- **Arquitetura MVVM**: Introdução de `SwipeViewModel` e `MediaRepository` para desacoplar a interface das regras de negócio e consultas ao MediaStore.
- **Sobrevivência a Rotações**: Estado da galeria, itens atuais, modos de ordenação e histórico de Undo preservados através do ciclo de vida.
- **Filtro por Pasta/Álbum**: Permite selecionar pastas específicas do dispositivo (Camera, WhatsApp, Screenshots, etc.) diretamente na tela de swipe.
- **Multi-seleção na Lixeira**: Suporte a seleção em lote no `TrashFragment` para restaurar ou excluir permanentemente vários itens de uma vez.
- **Identificação de GIFs**: Suporte a animação com badge visual `GIF` na interface de swipe.
- **Testes Unitários**: Criação da suíte de testes com JUnit (`AppUnitTest.kt`) cobrindo modelos, constantes e cálculos de expiração de lixeira.
- **Documentação de Contribuição**: Criação de `CONTRIBUTING.md` e `CHANGELOG.md`.

### 🔄 Modificado
- **Refatoração do SwipeFragment**: Redução de ~930 linhas para ~450 linhas com delegação ao ViewModel.
- **Room Migration**: Alterada política de migração para `fallbackToDestructiveMigrationOnDowngrade()`, prevenindo exclusão acidental do banco local.
- **Centralização de Constantes**: Criação de `Constants.kt` e `PrefsKeys.kt` para eliminar strings mágicas espalhadas.
- **Utilitário de Mídia**: Adicionada função `buildMediaUri` unificando a montagem de URIs do MediaStore em um único lugar.
- **Strings de Navegação**: Correção e adição de strings faltantes para suporte ao Navigation Drawer (`navigation_drawer_open`, `navigation_drawer_close`).

### 🔒 Segurança & Limpeza
- **Remoção de Senhas Hardcoded**: Remoção da senha exposta no `build.gradle`, adotando propriedades Gradle locais (`findProperty`).
- **Limpeza do Repositório Git**: Remoção de keystores de release/debug e logs de crash do rastreamento do Git.
- **Remoção de Dependências Desnecessárias**: Remoção de `okhttp` e integração desnecessária do Glide.

---

## [1.0.13] - 2026-10-20
- Versão de produção inicial publicada na Google Play Store.
- Swipe para manter ou descartar fotos e vídeos.
- Lixeira interna com retenção temporária de 30 dias.
- Limpeza periódica em segundo plano com WorkManager.
- Contador de espaço liberado e galeria de fotos mantidas.
