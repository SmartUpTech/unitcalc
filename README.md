# Unit Converter (Graphy Calculator)

A simple Android app that helps you **calculate numbers**, **convert units**, and **check currency rates** — all in one place.

It is being upgraded into **Graphy Calculator**: the same fast tools you already use, plus a future **Graphy** view that visually explains *how* an answer was worked out.

---

## What can this app do?

| Tab | What it is for |
|-----|----------------|
| **Calculator** | Everyday maths — add, subtract, multiply, divide, percentages, powers, square roots, and more |
| **Convert** | Change values between units (length, weight, temperature, etc.) and between world currencies |
| **Explore** | A home for more calculators and tools *(coming soon)* |
| **Favorites** | Save the calculators you use most *(coming soon)* |
| **History** | See your past calculator results in one list |

You can open **Settings** from the menu at the top of the screen (three dots).

---

## Calculator — examples

Open the **Calculator** tab and type like you would on a normal phone calculator.

| You type | You get |
|----------|---------|
| `25 + 17` | `42` |
| `100 − 35` | `65` |
| `6 × 7` | `42` |
| `50%` | `0.5` (half) |
| `2 + 3 × 4` | `14` *(multiplication is done first, like in school)* |
| `sqrt(16)` | `4` |

**Helpful tips**

- Tap **=** to finish a calculation.
- Tap the **history icon** on the calculator screen to see recent results from that session.
- Tap **copy** to copy the answer to your clipboard.
- Your results are also saved in the **History** tab at the bottom.

---

## Convert — examples

Open the **Convert** tab. You will see two choices at the top:

### Unit

Convert between everyday measurements.

| Example | How |
|---------|-----|
| Kilometres → miles | Choose **Length**, enter `5` km, pick **Mile** — you get about **3.11 miles** |
| Celsius → Fahrenheit | Choose **Temperature**, enter `100`, pick **Fahrenheit** — you get **212°F** |
| Kilograms → pounds | Choose **Weight**, enter `70` kg, pick **Pound** — you get about **154 lb** |

Other categories include area, volume, speed, time, cooking, storage, energy, and more.

### Currency

Convert money using live exchange rates (internet connection required).

| Example | How |
|---------|-----|
| USD → INR | Pick **US Dollar** and **Indian Rupee**, enter an amount, and see the converted value |
| Swap direction | Use the swap button to flip “from” and “to” currencies |

---

## History — examples

Every time you press **=** on the calculator, the expression and result are saved.

| What you see | What it means |
|--------------|---------------|
| `250 + 12%` → `280` | You worked out 250 plus 12 percent |
| `2 + 3 × 4` → `14` | A previous maths problem and its answer |

You can clear the full list with the delete button on the History screen.

---

## Settings

In **Settings** you can:

- Switch between **light** and **dark** theme
- Choose how many **decimal places** to show
- Pick your preferred **number separators** (e.g. `1,000.50` vs `1.000,50`)
- Rate or share the app

---

## What is Graphy?

**Graphy** is the visual side of the app. Instead of only showing a final number, Graphy shows a simple flowchart of the steps so you can understand *why* the answer is what it is.

**Example**

> You calculate `2 + 3 × 4` and get `14`.  
> Tap **Graphy** on the calculator screen to see: first `3 × 4 = 12`, then `2 + 12 = 14`.

After you press **=**, a **Graphy** chip appears in the top-right of the calculator display. Tap it to open the step-by-step flowchart; tap again to hide it.

---

## Who made this?

**SmartUp** — [smartuptech.github.io](https://smartuptech.github.io)

---

## For developers

- **Platform:** Android (Java)
- **Package:** `net.smartlogic.unitconverter`
- **Build:** `./gradlew assembleDebug`
- **Tests:** `./gradlew test`

Project planning and tasks are tracked in Notion under **Graphy Calculator**.
