# Generated — do not edit

`mtg-search.mjs` and `mtg-search.d.mts` are the Kotlin `:mtg-search` module (the card search
language) compiled to JavaScript. The server runs the same module on the JVM, so a query means
the same thing in both places.

To change search behaviour, edit `mtg-search/src/commonMain` and run `just search-js`, then commit
both. `:mtg-search:test` (`verifyWebClientSearch`) fails when this copy is stale. The web-client
reaches it only through `src/components/deckbuilder/query/index.ts`.
