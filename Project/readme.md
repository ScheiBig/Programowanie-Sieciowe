### Programowanie sieciowe
# Projekt Końcowy
### Serwer czasu

Projekt napisany został w języku Kotlin, korzystając
z infrastruktury/ekosystemu JVM dla Java 21.

Frontend aplikacji ma formę GUI w technologii JavaFX, backend natomiast opiera
się na zapewnionych przez Kotlin korutynach — ich wykorzystanie widać
w 3 głównych kontekstach:
* korutyny IO — daemony obsługujące ruch sieciowy,
* kotutyny asynchroniczne IO — głównie jako mechanizm komunikacji pomiędzy 
  daemonami,
* korutyny GUI — zlecane do wykonania na wątku głównym, pozwalają wykonywać
  atomowe operacje wpływające na interfejs.

Jako mechanizm komunikacji *GUI → logika biznesowa* używane są callbacki,
natomiast *logika biznesowa → GUI* korzysta z callbacków wyeksponowanych
przez klasy GUI (do prostych sygnałów), jak i obserwowalnych właściwości
przekazanych do GUI (do przekazywania danych).

Do celów prezentacji, pula wątków używana do zlecania korutyn IO została
na serwerze zwiększona z 64 do 256 wątków — w aplikacjach produkcyjnych,
ta liczba może wymagać bardziej precyzyjnego wyboru w zależności od założeń
programu (w tym przypadku — oczekiwanej liczby połączeń).

Uproszczona struktura projektu (lista nie jest w 100% alfabetyczna, aby poprawić czytelność):
```
[src/main/kotlin/com/marcinjeznach] - katalog główny kodów źródłowych
├╴[javafx] - narzędzia wspomagające do pracy z JavaFX
│ │
│ ├╴[application.kt] - funkcja do uruchamiania aplikacji JavaFX w sposób
│ │         idiomatyczny dla Kotlina
│ ├╴[DeepCollection.kt] - klasa mapująca obserwowalną tablicę obserwowalnych 
│ │         list, na obserwowalną wartość listy - pozwala na propagację eventów
│ │         zagnieżdżonych obiektów obserwowalnych (używana jako mapowanie słownika
│ │         interfejsów sieciowych do list klientów w panelu bocznym serwera)
│ ├╴[growProps.kt] - właściwości rozszerzające, które delegują ustawianie flag
│ │         sterujących zajmowaniem pozostałego miejsca przez węzły w layoutach
│ ├╴[insets.kt] - funkcje pomocnicze pozwalające na tworzenie obiektów
│ │         Insets w konwencji CSS
│ ├╴[message.kt] - funkcja tworząca sformatowane wiadomości do wyświetlanych
│ │         dzienników
│ ├╴[nodes.kt] - funkcje definiujące DSL do tworzenia widoków JavaFX,
│ │         zastępujący FXML wygodną składnią (inspirowane https://ktor.io/docs/server-html-dsl.html)
│ ├╴[ObservableInitializableSet.kt] - klasa dziedzicząca z ObservableSet,
│ │         pozwalająca na inicjalne załadowanie obiektów metodą addAll(), które
│ │         wyemituje event obserwatorom tylko dla ostatniego dodanego elementu
│ │         (domyślnie eventy są uruchamiane dla każdego elementu w addAll(),
│ │         co powoduje początkową desynchronizację stanu GUI względem aplikacji)
│ ├╴[paint.kt] - rozszerzenie pozwalające używać kolorów jako wypełnień tła węzłów,
│ │         bez tworzenia nieczytelnego zagnieżdżenia obiektów tła
│ ├╴[quit.kt] - funkcja delegująca zamknięcie aplikacji, zwracająca oczekiwany
│ │         typ Nothing, co pozwala kompilatorowi na dodatkowe sprawdzenia martwych
│ │         gałęzi kodu
│ └╴[separators.kt] - funkcje tworzące gotowe węzły, które mogą być używane jako
│           separatory w layoutach kolumnowych / wierszowych
│
├╴[networking] - narzędzia wspomagające pracę z prymitywmi sieciowymi
│ │
│ ├╴[interfaceAddress.kt] - rozszerzenie pozwalające wydobyć address IPv4
│ │         tekstowej formie dla interfejsu sieciowego
│ └╴[retrieveAvailableMulticastInterfaces.kt] - funkcja pozwalająca na pozyskanie
│           interfejsów sieciowych zdolnych do komunikacji multicast. Funkcja
│           pobiera listę dostępnych interfejsów, wstępnie odsiewa ją na podstawie
│           heurystycznych sygnałów, po czym testuje pozostałe interfejsy pod kątem
│           możliwości dołączenia do grupy multicastowej, odrzucając niezdolne 
│ 
├╴[utils] - różnorodne narzędzia do pracy z kodem
│ │
│ ├╴[atomics.kt] - funkcja przeładunku operatora, pozwalające skrócić zapis korzystania 
│ │         z atomowej zmiennej boolean
│ ├╴[connection.kt] - stała zawierająca znak ETX (\u0003). Znak ten używany jest 
│ │         w komunikacji sieciowej w całym programie jako zakończenie wiadomości,
│ │         zwiększając niezawodność komunikacji
│ ├╴[coroutines.kt] - funkcje pozwalające uruchamiać nowe korutyny, w zapisie znacznie  
│ │         bardziej zwięzłym niż domyślny (np. runIO {} vs GlobalScope.launch(Dispatchers.IO) {})
│ ├╴[datetime.kt] - funkcje ułatwiające pobieranie dat jako UNIX Epoch Timestamp,  
│ │         oraz ich formatowanie do formatu ISO 8601 (ber rozdzielającego 'T')
│ ├╴[delimitedStream.kt] - dekoratory na strumienie danych, pozwalające na bezpieczny
│ │          odczyt / zapis wiadomości zakończonych wybraną sekwencją znaków
│ └╴[print.kt] - funkcje pozwalające na drukowanie do StdErr, w sposób równie zwięzły  
│           co funkcje print(ln) (drukujące do StdOut)
│
├╴[project] - pakiet główny projektu
│ │         
│ ├╴[common] - API współdzielone przez klienta/serwer
│ │ │         
│ │ ├╴[conf.kt] - stałe dotyczące kofiguracji grupy multicastowej        
│ │ └╴[CommunicationMessage.kt] - API definiujące wiadomości wysyłane przez sieć 
│ │         między aplikacjami, oraz ich serializacji / deserializacji 
│ │ 
│ ├╴[client] - aplikacja kliencka (urządzenie badawcze)
│ │ │         
│ │ ├╴[Config.kt] - klasa pomocnicza do używania Preferences API do zapisu ustawień        
│ │ │       klasa pomocnicza do używania Preferences API do zapisu ustawień aplikacji
│ │ ├╴[ConnectionManager.kt] - klasa sterująca komunikacją UDP & TCP z serwerem
│ │ ├╴[Main.kt] - klasa główna aplikacji klienckiej 
│ │ └╴[MainView.kt] - klasa GUI aplikacji klienckiej 
│ │         
│ └╴[server] - aplikacja serwerowa (serwer czasu)
│   │         
│   ├╴[initializeMulticastListeners.kt] - klasa zarządzająca komunikacją UDP
│   │       oraz daemonami połączeń z danymi klientami
│   ├╴[initializeTCPListeners.kt] - klasa zarządzająca komunikacją TCP
│   │       oraz daemonami połączeń z danymi klientami
│   ├╴[ConnectionManager.kt] - klasa sterująca komunikacją UDP & TCP z serwerem
│   ├╴[Main.kt] - klasa główna aplikacji klienckiej
│   └╴[MainView.kt] - klasa GUI aplikacji klienckiej
│ 
│
└ [Main.kt] - główny program (dodany wyłącznie na potrzeby pakowania
            archiwum JAR - jako punkt wstępu do aplikacji)
```
