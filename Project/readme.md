### Programowanie sieciowe
# Wielowątkowość i wywołania asynchroniczne

Zadania wymagające wątków, używają zamiast tego korutyn uruchamianych na 
kontekście IO – jest to wbudowana pula wątków (chyba ok. 64) służąca
zazwyczaj do uruchamiania blokujących (zamiast przerywających – w świecie 
korutyn) zadań, które normalnie mogłyby zagłodzić główny wątek programu.

Użycie ww. kontekstu powinno gwarantować, że wszystkie 10 korutyn będzie
pracować jednocześnie, gwarantując efekt wielowątkowości.

Użycie wątków nie pozwala obecnie uzyskać dostępu do metod `Thread.pause()` /
`Thread.resume()` / `Thread.stop()` - od przynajmniej wersji Java 8 metody
te oznaczone są jako `@Deprecated`, a w wersji Java 21 używanej jako target
w tym projekcie, oznaczone jako API do usunięcia – metody te pozwalały na
niekooperacyjne zarządzanie cyklem życia wątku – mogły więc zatrzymać wątek,
który znajdował się w sekcji krytycznej, lub posiadał niezamknięte zasoby,
w rzadkich sytuacjach doprowadzając do nieodzyskiwalnych zakleszczeń (jest
to oficjalny powód ich usunięcia) – korutyny nastawione są na sterowanie
kooperacyjne, dlatego pozwalają na wygodniejsze zbudowanie własnej abstrakcji,
jak i są dużo lepiej wspierane w bibliotece standardowej (oraz bibliotekach
oficjalnych) Kotlina.
