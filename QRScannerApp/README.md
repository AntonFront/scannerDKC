# QR & Barcode Scanner App

Приложение для сканирования QR и штрих-кодов на Android, написанное на Kotlin.

## Возможности

- 📷 Сканирование QR кодов
- 📊 Сканирование штрих-кодов (EAN-13, EAN-8, UPC-A, Code 128, Code 39 и др.)
- ✨ Анимация лазерной линии при сканировании
- 📋 Копирование результата в буфер обмена
- 🎨 Современный Material Design интерфейс
- 🔦 Определение типа отсканированного кода

## Технологии

- **Kotlin** - основной язык разработки
- **CameraX** - работа с камерой
- **ML Kit Barcode Scanning** - распознавание кодов от Google
- **Material Components** - современный UI
- **ViewBinding** - безопасная работа с views

## Требования

- Android 7.0 (API 24) и выше
- Камера с автофокусом (рекомендуется)

## Установка

1. Откройте проект в Android Studio
2. Дождитесь синхронизации Gradle
3. Подключите Android устройство или запустите эмулятор
4. Нажмите Run (Shift+F10)

## Структура проекта

```
app/
├── src/main/
│   ├── java/com/example/qrcanner/
│   │   └── MainActivity.kt          # Основная активность
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml    # Разметка интерфейса
│   │   ├── values/
│   │   │   ├── colors.xml           # Цвета приложения
│   │   │   ├── strings.xml          # Строковые ресурсы
│   │   │   └── themes.xml           # Темы и стили
│   │   ├── drawable/
│   │   │   ├── scanning_frame.xml   # Рамка сканирования
│   │   │   └── ic_launcher_foreground.xml
│   │   └── anim/
│   │       └── laser_animation.xml  # Анимация лазера
│   └── AndroidManifest.xml
├── build.gradle.kts                  # Зависимости приложения
└── proguard-rules.pro
```

## Как это работает

1. Приложение запрашивает разрешение на использование камеры
2. CameraX предоставляет превью камеры и анализирует кадры
3. ML Kit Barcode Scanning распознаёт QR и штрих-коды в реальном времени
4. При успешном сканировании результат отображается в карточке
5. Результат можно скопировать в буфер обмена одной кнопкой

## Поддерживаемые форматы

- QR Code
- EAN-13 / EAN-8
- UPC-A / UPC-E
- Code 128 / Code 39 / Code 93
- ITF (Interleaved 2 of 5)
- Aztec
- Data Matrix
- PDF417

## Лицензия

MIT License
