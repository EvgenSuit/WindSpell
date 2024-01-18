
# Wind Spell

Wind Spell is a weather app developed with the help of the following technologies:

- Kotlin Compose
- Coroutines
- Retrofit
- Room
- SQL
- Open Weather API
- Internet ConnectivityManager
- JUnit
 
It supports the following languages, where each is set according to a user's location:
- English
- Polish
- Belarusian
- Russian

**In the recent update the UI reflects the daytime of the searched city(it gets brighter when it's day, and darker when it's night)**

https://github.com/EvgenSuit/WindSpell/assets/77486483/2f7d2062-b7a4-4542-acef-d36ddd47d769


https://github.com/EvgenSuit/WindSpell/assets/77486483/bbc3bf1b-8be4-49fb-9591-f645649e6104





## Run Locally

Clone the project
```
git clone https://github.com/EvgenSuit/WindSpell.git
```

Go to the project directory
```
cd WindSpell
```

Go to _app/src/main/java/com/example/windspell_, create a file named _confit.kt_ and insert the following code
```
package com.example.windspell

class Config {
    companion object {
        const val openWeatherApiKey = "open_weather_api_key"
    }
}
```
**An API key will be provided by me personally in case such a request is made**

Run in android studio
## Note

- In order for the language update to take place, the reload is required.

- **The minimum Android version supported is 8.0**

- Apk file is located at the root of the project directory
