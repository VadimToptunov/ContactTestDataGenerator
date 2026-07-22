# Contact Generator 📇

Generate test contacts in VCF format for testing purposes.

## Features ✨

- 📄 **VCF File Generation** - Creates standard vCard 3.0 files
- 🌍 **Multilingual Support** - 4 languages (English, Russian, Ukrainian, Spanish)
- 🎨 **Modern UI** - Built with Jetpack Compose and Material Design 3
- 📤 **Easy Sharing** - Share generated VCF files with any app
- 🚀 **Fast Generation** - Create up to 10,000 contacts
- 📱 **Google Play Ready** - No dangerous permissions required

## Screenshots

[Add screenshots here]

## How It Works

1. Enter the number of contacts you want to generate (1-10,000)
2. Tap "Generate Contacts"
3. Wait for the generation to complete
4. Share the VCF file with any contacts app or save it

## Generated Contact Data

Each contact includes:
- ✅ Full name (first + last)
- ✅ Phone number (US format)
- ✅ Email address
- ✅ Company name
- ✅ Job title

## Supported Languages

- 🇬🇧 English
- 🇷🇺 Russian
- 🇺🇦 Ukrainian
- 🇪🇸 Spanish

See [LOCALIZATION.md](LOCALIZATION.md) for details.

## Technical Details

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 14+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with ViewModel

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on emulator or device

```bash
git clone https://github.com/yourusername/ContactTestDataGenerator.git
cd ContactTestDataGenerator
./gradlew assembleDebug
```

## Permissions

This app requires **NO dangerous permissions**! 🎉

- No READ_CONTACTS
- No WRITE_CONTACTS
- Files are stored in app's private directory

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

Vadim Toptunov

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

If you find this app useful, please consider:
- ⭐ Starring the repository
- 🐛 Reporting bugs
- 💡 Suggesting new features
- 🌍 Adding new translations

---

Made with ❤️ using Jetpack Compose

