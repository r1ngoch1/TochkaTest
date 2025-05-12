import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

class Event implements Comparable<Event> {
    LocalDate date;
    int type; // 

    public Event(LocalDate date, int type) {
        this.date = date;
        this.type = type;
    }

    @Override
    public int compareTo(Event other) {
        int dateComparison = this.date.compareTo(other.date);
        if (dateComparison != 0) {
            return dateComparison;
        }

        return Integer.compare(this.type, other.type);
    }

    @Override
    public String toString() {
        return "Event{" +
                "date=" + date +
                ", type=" + type +
                '}';
    }
}

public class run {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Первая строка - вместимость гостиницы
        int maxCapacity = Integer.parseInt(scanner.nextLine());

        // Вторая строка - количество записей о гостях
        int n = Integer.parseInt(scanner.nextLine());

        List<Map<String, String>> guests = new ArrayList<>();

        // Читаем n строк, json-данные о посещении
        for (int i = 0; i < n; i++) {
            String jsonGuest = scanner.nextLine();
            // Простой парсер JSON строки в Map
            Map<String, String> guest = parseJsonToMap(jsonGuest);
            guests.add(guest);
        }

        // Вызов функции
        boolean result = checkCapacity(maxCapacity, guests);

        // Вывод результата
        System.out.println(result ? "True" : "False");

        scanner.close();
    }

    public static boolean checkCapacity(int maxCapacity, List<Map<String, String>> guests) {
        List<Event> events = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        for (Map<String, String> guest : guests) {
            LocalDate checkInDate = LocalDate.parse(guest.get("check-in"), formatter);
            LocalDate checkOutDate = LocalDate.parse(guest.get("check-out"), formatter);

            events.add(new Event(checkInDate, 1));

            events.add(new Event(checkOutDate, -1));
        }

        Collections.sort(events);

        int currentOccupancy = 0;
        for (Event event : events) {
            if (event.type == 1) {
                currentOccupancy++;
                if (currentOccupancy > maxCapacity) {
                    return false;
                }
            } else {
                currentOccupancy--;
            }
        }

        return true;
    }


    // Вспомогательный метод для парсинга JSON строки в Map
    private static Map<String, String> parseJsonToMap(String json) {
        Map<String, String> map = new HashMap<>();
        // Удаляем фигурные скобки
        json = json.substring(1, json.length() - 1);

        // Разбиваем на пары ключ-значение
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2); // Limit split to 2 parts
            String key = keyValue[0].trim().replace("\"", "");
            String value = keyValue[1].trim().replace("\"", "");
            map.put(key, value);
        }
        return map;
    }


}