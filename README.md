# Yen to Dollar

A dark, clean Japanese yen → US dollar calculator, in two forms:

- **Web app** (`index.html`): a live calculator you can install from Chrome. It fetches the current rate and still works offline with the last saved rate.
- **Android app** (`android/`): the same calculator plus a home screen widget that shows ¥1,000 / ¥5,000 / ¥10,000 / ¥50,000 in dollars and refreshes on its own every few hours.

Rates come from the free [ExchangeRate-API open endpoint](https://www.exchangerate-api.com/docs/free), which updates about once a day. They are mid-market rates; banks and card fees will differ.

## Use the web app

1. Turn on GitHub Pages: **Settings → Pages → Build and deployment → Source: Deploy from a branch → Branch: `main`, folder `/ (root)` → Save**.
2. After a minute, open `https://breadsmeow.github.io/currency-calculator/` in Chrome.
3. Tap **⋮ → Add to home screen → Install**.

## Install the Android app and widget

Every push that changes `android/` builds the app automatically (see the **Actions** tab).

1. On your phone, open the [latest release](https://github.com/breadsmeow/currency-calculator/releases/tag/latest) and tap **YenDollar.apk**.
2. Open the downloaded file. If Android asks, allow your browser to install unknown apps.
3. Long-press the home screen → **Widgets** → **Yen to Dollar**, then pick one (or both):
   - **Yen calculator** (4×4): a full keypad on the home screen. Type an amount and the conversion updates right there. ⇅ switches between yen→dollars and dollars→yen, C clears, and the decimal point works in dollar mode.
   - **Yen rates** (4×2): ¥1,000 / ¥5,000 / ¥10,000 / ¥50,000 in dollars at a glance.

Tap ↻ on either widget to refresh the rate, or tap the top of it to open the full app.

## Notes

- `android/signing/yendollar.keystore` is a throwaway signing key kept in the repo so each new build installs over the previous one. It is only suitable for a personal, sideloaded app.
- To build on a computer instead: open the `android/` folder in Android Studio and press Run.
