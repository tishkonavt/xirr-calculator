# XIRR Calculator

Кратко  
Консольное приложение на Kotlin для расчёта XIRR по данным портфеля из CSV, генерации двух PNG-графиков и создания CSV с добавленными столбцами xirr_annual и xirr_cumulative.

Требования
- Java 17
- Gradle (необязательно — есть Gradle wrapper ./gradlew)

Ожидаемый CSV (по умолчанию `portfolio_data.csv` в рабочей директории)
- Обязательно наличие колонки с датой — имя столбца может содержать "date" (например: `date`).
- Рекомендуемые столбцы (используются в проекте):  
  `date,valuation,cashIn,cashOut`  
  Формат даты: `yyyy-MM-dd`  
  Значения числовые: целые или с дробной частью (точка как разделитель).

Пример файла (первая строка — заголовок):
```
date,valuation,cashIn,cashOut
2016-12-29,1000000,0,0
2017-01-02,1005000,10000,0
...
```

Запуск
1) Быстрый запуск из исходников (находясь в корне проекта):
```bash
./gradlew :app:run --args="portfolio_data.csv"
```

2) Собрать fat JAR и запустить:
```bash
./gradlew :app:shadowJar
java -jar app/build/libs/xirr-calculator.jar portfolio_data.csv
```

3) Альтернативно задать путь через переменную окружения:
```bash
export XIRR_CSV_PATH=path/to/portfolio_data.csv
./gradlew :app:run
```

Выходные файлы (в рабочей директории)
- xirr_annual.png — график годовой доходности  
- cumulative_return.png — график кумулятивной доходности  
- <orig>_with_xirr.csv — копия исходного CSV с добавленными колонками `xirr_annual` и `xirr_cumulative` (значения в виде десятичных долей, например 0.1234 = 12.34%)

Рекомендации перед отправкой другу
- Оставьте в репозитории: `app/src/...`, `app/src/main/resources/portfolio_data.csv` (пример), `gradlew`, `gradle/wrapper`, `README.md`.
- Исключите из репозитория сгенерированные артефакты (build/, app/bin/, app/build/ и т.д.) — пример .gitignore ниже.