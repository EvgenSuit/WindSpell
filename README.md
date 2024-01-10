
# Wind Spell

Wind Spell is a weather app developed with the help of the following technologies:

- Kotlin Compose
- Coroutines
- Retrofit
- Room
- SQL
- Open Weather API
- Internet ConnectivityManager
 
It supports the following langugages, where each is set according to a user's location:
- English
- Polish
- Belarusian
- Russian

https://github.com/EvgenSuit/WindSpell/assets/77486483/9e4e4942-163f-4a81-ae57-e05b320b0b1a


https://github.com/EvgenSuit/WindSpell/assets/77486483/233d5b18-1ba2-4fd3-afe3-742cd0711c68



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
