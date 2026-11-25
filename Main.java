import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // Запуск примера, демонстрирующего совместное использование ресурсов и интерференцию потоков
        (new InterferenceExample()).example();
    }
}


class RandomRunExample extends Thread {
    public void run(){
        System.out.println(Thread.currentThread().getName());
    }

    public static void example(){
        // Создает и запускает 10 независимых потоков.
        for (int i = 0; i < 10; i++) {
            Thread thread = new RandomRunExample();
            thread.start();
        }
    }
}

class SeriesRunExample extends Thread {
    private static int currentMax = 1;
    private int mainId; // Уникальный идентификатор данного потока
    private final Object waitObject; // Объект, используемый для синхронизации потоков

    private SeriesRunExample(int mainId, Object waitObject) {
        this.mainId = mainId;
        this.waitObject = waitObject;
    }
    public static void example(){
        Object waitObject = new Object();
        // Создаем и запускаем потоки с идентификаторами от 1 до 9
        for (int i = currentMax; i < 10; ++i) {
            Thread thread = new SeriesRunExample(i, waitObject);
            thread.start();
        }
    }

    public void run() {
        System.out.println("Стартовал поток " + mainId);
             synchronized (waitObject){
                        while (mainId > currentMax){
                try {
                    // wait() приостанавливает выполнение потока до вызова notify/notifyAll
                    waitObject.wait();
                    System.out.println("После wait'a " + mainId);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // Как только mainId == currentMax, поток выходит из цикла while и продолжает выполнение.
            currentMax++; // Увеличиваем ожидаемый идентификатор для следующего потока

            System.out.println("Отработал поток " + mainId);
            // notifyAll() пробуждает все ждущие потоки.
            waitObject.notifyAll();
        }
    }
}

class StateObject {
    private int i; // Значение счетчика

    // Ключевое слово synchronized гарантирует, что только один поток может выполнять этот метод
    synchronized void increment(){
        i++;
    }

    // Метод получения текущего значения счетчика.
    public int getI(){
        return i;
    }
}
class InterferenceExample {
    // Лимит операций инкремента (100 миллионов)
    private static final int HUNDRED_MILLION = 100000000;

    // Потокобезопасный счетчик для контроля общего количества итераций всех потоков
    private AtomicInteger counter = new AtomicInteger();

    // Объект, содержащий состояние, которое будет обновляться из разных потоков
    private StateObject stateObject = new StateObject();

    // Метод условия остановки: атомарно увеличивает счетчик и проверяет, не превышен ли лимит.
    boolean stop() {
        return counter.incrementAndGet() > HUNDRED_MILLION;
    }

    public void example() throws InterruptedException {
        // Создаем два рабочих потока, передавая им общие экземпляры checker и stateObject
        InterferenceThread thread1 = new InterferenceThread(this, stateObject);
        InterferenceThread thread2 = new InterferenceThread(this, stateObject);

        // Запускаем оба потока параллельно
        thread1.start();
        thread2.start();

        // Основной поток ожидает завершения thread1, thread2
        thread1.join();
        thread2.join();

        // Выводим ожидаемое количество инкрементов
        System.out.println("Expected: " + HUNDRED_MILLION);
        // Выводим фактический результат, благодаря синхронизации в StateObject, количество успешных инкрементов точно совпадет с ожидаемым
        System.out.println("Result: " + stateObject.getI());
    }
}

// Класс потока, который выполняет инкремент общего состояния до момента остановки
class InterferenceThread extends Thread {
    private final InterferenceExample checker; // Ссылка на объект для проверки условия остановки
    private final StateObject stateObject;     // Ссылка на объект, состояние которого нужно изменять

    InterferenceThread(InterferenceExample checker, StateObject stateObject) {
        this.checker = checker;
        this.stateObject = stateObject;
    }

    public void run() {
        // Цикл работает, пока checker.stop() возвращает false, как только 100_000_001-й поток вызовет stop()
        while (!checker.stop()) {
            // Потокобезопасное увеличение значения
            stateObject.increment();
        }
    }
}