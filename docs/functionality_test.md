# Test des Funktionsumfangs des PURL-Servers

1) Installation
   - DB Schema anlegen
   - DB Einstellungen in Properties
   - Anwendung starten
   -> Prüfen das die Tabellen und der Admin-User angelegt sind
   
2) Ersteinrichtung
   - Passwort des Admin-Nutzers ändern
     a) per GUI -> User bearbeiten
     b) per Zurücksetzen via E-Mail

3) Domains erstellen / bearbeiten
    - neuen Nutzer "user1" erstellen
    - neuen Nutzer "user2" erstellen
    - neue Domain "/web" erstellen (user1 und user2 als Berechtigte eintragen)
    - Domain suchen / verschiedene Felder bearbeiten / Ergebnisse prüfen
    
4) PURL erstellen (GUI):
    - neue PURL /web/google -> https://google.uni-rostock.de erstellen
    - neue PURL /web/ubr -> https://www.ub.uni-rostock.de erstellen
    - PURLs im Browser testen:
      -  http://localhost:8080/web/google
      -  http://localhost:8080/web/ubr

5) Nutzer user1 löschen   
   - versuchen als user1 - eine Domain / PURL zu ändern
     -> Fehlermeldungen auf Verständlichkeit prüfen
   - Nutzer user1 wieder reaktivieren -> erneut esten
   
6) Partial PURLs testen
   - neue Domain anlegen: /rosdok_test
   - neue PURL anlegen: /rosdok_test/doberan
      -> https://rosdok.uni-rostock.de/resolve/id/rosdok_document_0000014983
   - neue Partial-PURL anlegen: rosdok_test/doberan/
     -> https://rosdok.uni-rostock.de/resolve/id/rosdok_document_0000014983/image/part/

   - testen: https://localhost:8080/rosdok_test/doberan
     -> Anzeige RosDok mit Titelseite
   - testen: https://localhost:8080/rosdok_test/doberan/phys_0021
     -> Anzeige RosDok mit geöffnetem Inhaltsverzeichnis
   - testen https://localhost:8080/rosdok_test/doberan/phys_0021
     -> Anzeige RosDok mit Kapitel "Die Kirche zu Doberan"
     
7) API Testen
  - ähnlich wie zuvor die REST-API testen
  - eigene Java-Klasse schreiben auf Basis von:    
     /uni/rostock/ub/purl_server/client/PurlClient.java
