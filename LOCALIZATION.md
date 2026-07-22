# Localization Guide

This application supports automatic device language detection with fallback to English.

## Supported Languages

| Language | Code | Direction | Status |
|----------|------|-----------|--------|
| English | `en` (default) | LTR | ✅ Complete |
| Russian | `ru` | LTR | ✅ Complete |
| Ukrainian | `uk` | LTR | ✅ Complete |
| Spanish | `es` | LTR | ✅ Complete |

**For all other languages**, the app automatically uses the English version (fallback).

> **Note:** Right-to-left (RTL) languages are intentionally not supported at the
> moment. `android:supportsRtl="false"` is set in the manifests, and the app
> ships only left-to-right locales.

## File Structure

```
app/src/main/res/
├── values/              # English (default) - used for all unsupported languages
│   └── strings.xml
├── values-ru/           # Russian
│   └── strings.xml
├── values-uk/           # Ukrainian
│   └── strings.xml
└── values-es/           # Spanish
    └── strings.xml
```

## How Localization Works

Android automatically selects the correct `strings.xml` file based on the device language:

- If device language is **Russian** → uses `values-ru/strings.xml`
- If device language is **Ukrainian** → uses `values-uk/strings.xml`
- If device language is **Spanish** → uses `values-es/strings.xml`
- For all other languages → uses `values/strings.xml` (English)

### Fallback Mechanism

If there is no dedicated folder for the device language, Android automatically uses `values/` (English):

```
Example: Device in German 🇩🇪
1. Android searches for: values-de/strings.xml
2. Not found ❌
3. Uses: values/strings.xml (English) ✅
```

This guarantees the app **always works**, even if the language is not directly supported.

### Fallback Examples

| Device Language | File Used | Note |
|----------------|-----------|------|
| 🇬🇧 English | `values/strings.xml` | Default |
| 🇷🇺 Russian | `values-ru/strings.xml` | Exact match |
| 🇺🇦 Ukrainian | `values-uk/strings.xml` | Exact match |
| 🇪🇸 Spanish | `values-es/strings.xml` | Exact match |
| 🇩🇪 German | `values/strings.xml` | Fallback (English) |
| 🇫🇷 French | `values/strings.xml` | Fallback (English) |
| 🇨🇳 Chinese | `values/strings.xml` | Fallback (English) |
| 🇯🇵 Japanese | `values/strings.xml` | Fallback (English) |

## Adding a New Language

1. Create directory `values-{language_code}/` in `app/src/main/res/`
2. Copy `values/strings.xml` to the new directory
3. Translate all strings (keep `name=` attributes, placeholders like `%d`/`%1$d`, and symbols unchanged)
4. Add the correct plural forms for the language (see below)
5. Update this file by adding the new language to the table

Examples of language codes:
- German: `values-de`
- French: `values-fr`
- Italian: `values-it`
- Chinese: `values-zh`

Only left-to-right languages are currently in scope. Adding an RTL language
would additionally require setting `android:supportsRtl="true"` again in the
manifests and re-testing the UI for mirroring.

## Implementation Details

### Plurals for Russian and Ukrainian

Both Russian and Ukrainian use 4 plural forms:
- `one` - 1 contact, 21 contacts, 31 contacts...
- `few` - 2-4 contacts, 22-24 contacts...
- `many` - 5-20 contacts, 25-30 contacts...
- `other` - fractional (1.5 contacts, 2.3 contacts...)

Example:
```xml
<plurals name="contacts_generated">
    <item quantity="one">%d contact generated successfully!</item>
    <item quantity="few">%d contacts generated successfully!</item>
    <item quantity="many">%d contacts generated successfully!</item>
    <item quantity="other">%d contacts generated successfully!</item>
</plurals>
```

Spanish uses 2 forms (`one`, `other`).

### Formatted Strings

For strings with parameters, use Java formatting:
- `%d` - integer
- `%1$d`, `%2$d` - positional parameters

Example:
```xml
<string name="progress_creating">%1$d / %2$d</string>
```

## Testing Localization

### On Emulator / Real Device

1. Settings → System → Languages & input → Languages
2. Add the desired language and move it to the first position
3. Restart the application

### Quick Switch for Development

In Android Studio:
1. Run → Edit Configurations
2. In the "General" tab find "Language"
3. Select the language for testing

## Common Issues and Solutions

### Problem 1: Text Not Translated
**Cause:** Hardcoded strings in code
**Solution:** All user-facing strings should be in `strings.xml`:
```kotlin
// Bad
Text("Generate")

// Good
Text(stringResource(R.string.generate_btn_text))
```

### Problem 2: Missing Translation lint error
**Cause:** A string exists in `values/strings.xml` but not in a locale file
**Solution:** Add the missing string (and correct plural forms) to every locale folder

## Useful Links

- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [Multilingual Support Best Practices](https://developer.android.com/guide/topics/resources/multilingual-support)

## Summary

The application is multilingual:
- ✅ 4 LTR languages (English, Russian, Ukrainian, Spanish)
- ✅ Correct plurals for all languages
- ✅ Fallback to English for unsupported languages
