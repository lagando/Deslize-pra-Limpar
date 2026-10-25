# Melhorias Pendentes — Deslize pra Limpar

> Gerado em 25/10/2026. Prioridades alta (segurança), README, Fase 2 (Qualidade de Código), Fase 3 (Arquitetura) e principais features da Fase 5 já foram aplicadas.

---

## ✅ Fase 1 — Segurança & Limpeza do Repositório (Concluída)
- [x] Senhas do keystore removidas de `build.gradle` (usando `findProperty`).
- [x] Regras de `.gitignore` para logs e binários.
- [x] Arquivos desnecessários removidos do Git.
- [x] README completamente reescrito e documentado.

---

## ✅ Fase 2 — Qualidade de Código (Concluída)
- [x] Constantes `MediaStatus` e `MediaType` criadas em `model/Constants.kt`.
- [x] Constantes de preferências `PrefsKeys` criadas em `data/PrefsKeys.kt`.
- [x] Utilitário `buildMediaUri` criado em `util/MediaUtils.kt`.
- [x] Dependência não utilizada do `okhttp` removida do `build.gradle` e `MyAppGlideModule` simplificado.
- [x] Chamadas de `context?.getString()` redundantes corrigidas.

---

## ✅ Fase 3 — Arquitetura (Concluída)
- [x] **MediaRepository**: Criado `data/MediaRepository.kt` desacoplando consultas ao MediaStore e Room.
- [x] **SwipeViewModel**: Criado `SwipeViewModel.kt` gerenciando itens, índices, modos de ordenação e Undo com `LiveData`, sobrevivendo a rotações de tela.
- [x] **SwipeFragment refatorado**: Reduzido de 929 linhas para ~450 linhas, delegando lógica de negócio ao ViewModel.
- [x] **Migrações seguras no Room**: Configurado `fallbackToDestructiveMigrationOnDowngrade()` para evitar apagamento acidental do histórico de mídias.

---

## ✅ Fase 4 — Documentação & Governança (Concluída)
- [x] **README.md**: Atualizado para refletir arquitetura MVVM, novas features, árvore de arquivos do projeto e testes.
- [x] **CONTRIBUTING.md**: Guia completo de contribuição, padrões de código Kotlin, convenções de commits e testes.
- [x] **CHANGELOG.md**: Histórico de versões seguindo o padrão *Keep a Changelog*.
- [x] **Correção de Strings de Navegação**: Adicionadas `navigation_drawer_open` e `navigation_drawer_close` prevenindo falhas de build.

---

## 🚀 Fase 5 — Features & Testes

### ✅ Já Implementadas:
- [x] **5.1 Filtro por álbum/pasta**: Opção no menu da toolbar que lista e filtra as fotos por pastas reais da galeria (Camera, WhatsApp, etc.).
- [x] **5.2 Multi-seleção na lixeira**: `TrashFragment` e `TrashAdapter` agora suportam seleção múltipla (long-press ou toque) para restaurar ou excluir permanentemente vários itens de uma vez.
- [x] **5.7 Suporte a GIFs**: Detecção automática de GIFs com badge específico e suporte a animação.
- [x] **5.8 Testes Unitários**: Criado `AppUnitTest.kt` com validações de constantes, modelos e cálculos de tempo do Worker de expiração.

### ⚪ Backlog para Futuras Iterações:
- [ ] **5.3 Pinch-to-zoom na tela de swipe**: Integração de zoom em tela cheia com ScaleGestureDetector sem conflitar com swipe.
- [ ] **5.4 Dashboard de estatísticas**: Histórico de limpeza visual com métricas acumuladas de GB liberados.
- [ ] **5.5 Tema claro alternativo**: Criação de variantes de cores para tema light.
- [ ] **5.6 Widget de tela inicial**: Atalho com contagem rápida de espaço a liberar.
