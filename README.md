# Weather_App

Weather_App is a coding challenge that was created within 4 days utilizing Kotlin, OkHttp with a third-party API, and RxJava.

## About
The app is relatively barebones with minimal firebase implementation (due to time constraints). It connects to a third-party weather API to display various data to the user.

Data includes: Current Weather (Low/High, Date, Wind Speed, Humidity, etc.) with placeholder values if the user does not allow GPS/Location services and 5 Day Forecast

The user is also able to search for cities in a city - city,state - city,state,country or city,country format.
```
Examples: London,UK  |  Cairo  |  New York,NY,US
```
States with similar names need a state and sometimes country modifier to get the exact location
```
Examples: Newark will return Newark,CA and not Newark,NJ
```
Additionally, the app allows the user to save specific cities locally to the app.
