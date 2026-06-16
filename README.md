# Busca de Cartas Magic — Versão com Permissão Android

## Descrição
Aplicativo Android que permite aos usuários buscar cartas do jogo Magic: The Gathering. [cite_start]O app oferece filtros por nome, identidade de cor e tipos de carta (Terrenos básicos e Criaturas), exibindo os detalhes principais e a arte da carta na interface. [cite_start]Nesta versão, o app evolui a partir da atividade anterior com a introdução de um sistema de alerta para monitoramento de preços utilizando permissões nativas.

## Relação com a atividade anterior
[cite_start]Na atividade original, a funcionalidade do miniapp era realizar a busca na API pública para exibir a imagem e os atributos da carta. [cite_start]Nesta nova versão, adicionamos um recurso para o usuário monitorar o valor de mercado (em dólares) da carta pesquisada, implementando o caso de uso real de permissão de notificação da plataforma Android.

## API utilizada
- Nome da API: Scryfall API 
- Endpoint utilizado: `/cards/random` 
- Dados exibidos no app: Nome da carta, custo de mana, linha de tipo, URIs das imagens e o preço (USD).

## Permissão Android utilizada
- Permissão escolhida: `POST_NOTIFICATIONS` 
- Onde ela foi declarada no Manifest: 
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
````

Como executar o projeto

Clonar este repositório.

Abrir o projeto no Android Studio.

Aguardar a sincronização do Gradle.

Executar em emulador ou dispositivo físico.

Testar a funcionalidade de API pesquisando os filtros.

Clicar no botão "Criar Alerta de Preço" para disparar a solicitação de permissão de notificação

Prints do Aplicativo:

Botão para criar o alerta no dispositivo:

<img width="738" height="1600" alt="image" src="https://github.com/user-attachments/assets/576ecd5b-8417-4703-931c-cefd4ad22df2" />

Notificação no dispositivo do usuário:

<img width="738" height="1600" alt="image" src="https://github.com/user-attachments/assets/c3d03501-7fff-4fa0-9a1b-e2d722b135a3" />


Autor:

Dyogo Antonio Silva Oliveira
