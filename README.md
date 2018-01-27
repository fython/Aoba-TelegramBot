Aoba Bot
======

## Introduce

A Telegram Bot written in [Kotlin](https://kotlinlang.org/) language. 
Use [rubenlagus/TelegramBots](https://github.com/rubenlagus/TelegramBots) as bot base library.
I made a wrapper to develop functions easily and quickly. 
In the future I may separate framework from this repository and publish under Apache 2.0 Licenses.

**Currently only support Chinese. It's inconvenient to support multi-language.**

## What functions it has now?

It's still in unstable development.

Functions:

- `/replace` : Replace a keyword with another word in a sentence
- `/choose` : Random choose a element in a list
- `/add_space` / `/remove_space` : Add or remove spaces between chinese characters
- `/allow_game` : Enable or disable bot games in a group (Need administrators)
- `/bot_statistics` : Show bot statistics and running status

Now it has three games you can play in group:

- `/guess_number_game` : Guess number
- `/bomb_game` : Deliver bomb (Sticker)
- `/minesweeper_game` : Minesweeper

## Configure Bot keystore

- Create `src/main/resources/keystore.properties`
- Add your `BOT_TOKEN` and `BOT_USERNAME` like this:
```properties
BOT_TOKEN=123456:XXXX
BOT_USERNAME=sb_bot
```

## Build and run

Intellij IDEA is optional. But I recommend use it to develop.

Use Gradle to build and run (on Linux) :

```bash
./gradlew run
```

For Windows users, you can also run `gradlew.bat` instead. 

## Contact me

Telegram: [@fython](https://t.me/fython)

Donate via Alipay: `316643843#qq.com` (Replace `#` with `@`)

Donate via PayPal: [https://paypal.me/fython](https://paypal.me/fython)

## License

GPL v3

```
GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007

Copyright (C) 2018 Fung Gwo (fython)

This program comes with ABSOLUTELY NO WARRANTY.
This is free software, and you are welcome to redistribute it under certain conditions.
```
