# Telegram widget

- `bun install`
- `bun run format` formats the HTML, CSS and JavaScript.
- `bun run optimize` writes `dist/index.html`.
- `bun run lint:digits` checks digit centerlines for unexpected crossings.
- `bun run report:digits` writes a Persian digit measurement and pair proof to
  `dist/digits-report.html`. Measurements are approximate, not an optimization.
- To check the result with Telegram's sandbox, set it up once:
  `git clone --depth 1 --branch v0.6.5 https://github.com/TelegramMessenger/widget-sandbox.git .sandbox`
  and `(cd .sandbox && bun install --production --filter '@telegram/widget-sandbox...')`.
- `bun run check` checks the optimized file's size and runs it through the sandbox.
  It reports diagnostics for code exercised during the sandbox run.
