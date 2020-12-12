import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class DoctorsOffice extends Thread {
    static int numberOfPatients;
    static int numberOfDoctors = 3;
    static int patientsWhoLeft = 0;
    static int patientsWhoVisit = 0;
    static int numberOfSeats = 3;
    static Clock clock;
    static Semaphore[] doctors = new Semaphore[]{new Semaphore(0),
            new Semaphore(0), new Semaphore(0)};
    static Semaphore Seats = new Semaphore(1);
    static Semaphore freeDoctor = new Semaphore(1);
    static Semaphore patients = new Semaphore(0);
    static Doctor[] allDoctors = new Doctor[numberOfDoctors];
    static Doctor[] freeDoctors = allDoctors;
    static int numberOfFreeDoctors = numberOfDoctors;

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.println("welcome to office with 3 doctors and 3 chairs in waiting room!");
        System.out.println("please enter number of patients: ");
        DoctorsOffice.numberOfPatients = input.nextInt();
        String[] names = new String[numberOfPatients];
        int[] times = new int[numberOfPatients];
        for (int i = 0; i < numberOfPatients; i++) {
            System.out.println("please enter name of patient: ");
            names[i] = input.next();
            System.out.println("when " + names[i] + " arrives?");
            times[i] = input.nextInt();
        }
        DoctorsOffice office = new DoctorsOffice();
        office.start();
        Message message = new Message();
        for (int i = 0; i < numberOfDoctors; i++) {
            allDoctors[i] = new Doctor(message, i);
        }
        clock = new Clock(message);
        for (int i = 0; i < numberOfPatients; i++) {
            Patient patient = new Patient(times[i], message, names[i]);
        }
    }
}

class Doctor extends Thread {
    final Message message;
    int num;

    public Doctor(Message msg, int num) {
        this.num = num;
        this.message = msg;
        this.start();
    }

    public void run() {
        while (true) {
            try {
                DoctorsOffice.patients.acquire();
                DoctorsOffice.doctors[num].release();
                this.treatment();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void treatment() throws InterruptedException {
        int t = DoctorsOffice.clock.time;
        while (DoctorsOffice.clock.time < t + 2) {
            synchronized (message) {
                message.wait();
            }
        }
    }
}

class Clock extends Thread {
    public int time = 0;
    final Message message;

    public Clock(Message msg) {
        this.message = msg;
        this.start();
    }

    public void run() {
        while (true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            time++;
            synchronized (message) {
                message.notifyAll();
            }
        }
    }
}

class Message {
    public Message() {
    }
}

class Patient extends Thread {
    String name;
    long timeArrive;
    final Message message;
    int doctorNum;

    public Patient(long timeArrive, Message message, String name) {
        this.timeArrive = timeArrive;
        this.message = message;
        this.name = name;
        this.start();
    }

    public void run() {
        while (DoctorsOffice.clock.time < timeArrive) {
            synchronized (message) {
                try {
                    message.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            DoctorsOffice.Seats.acquire();
            if (DoctorsOffice.numberOfSeats <= 0) {
                DoctorsOffice.Seats.release();
                System.out.println(this.name + " left the office because there is no chair to seat");
                DoctorsOffice.patientsWhoLeft++;  //at DoctorsOffice.clock.getTime()
            } else {
                DoctorsOffice.patients.release();
                sleep(1);
                DoctorsOffice.freeDoctor.acquire();
                if (DoctorsOffice.numberOfFreeDoctors <= 0) {
                    DoctorsOffice.freeDoctor.release();
                    DoctorsOffice.numberOfSeats = DoctorsOffice.numberOfSeats - 1;
                    DoctorsOffice.Seats.release();
                    DoctorsOffice.doctors[doctorNum].acquire();
                    DoctorsOffice.Seats.acquire();
                    DoctorsOffice.freeDoctor.acquire();
                    DoctorsOffice.numberOfFreeDoctors = DoctorsOffice.numberOfFreeDoctors - 1;
                    DoctorsOffice.freeDoctor.release();
                    DoctorsOffice.numberOfSeats = DoctorsOffice.numberOfSeats + 1;
                    DoctorsOffice.Seats.release();
                    this.visit(doctorNum);
                    DoctorsOffice.freeDoctor.acquire();
                    DoctorsOffice.numberOfFreeDoctors = DoctorsOffice.numberOfFreeDoctors + 1;
                    DoctorsOffice.freeDoctor.release();
                } else {
                    DoctorsOffice.Seats.release();
                    for (int i = 0; i < DoctorsOffice.numberOfDoctors; i++) {
                        for (int j = 0; j < DoctorsOffice.numberOfFreeDoctors; j++) {
                            if (DoctorsOffice.allDoctors[i] == DoctorsOffice.freeDoctors[j]) {
                                doctorNum = i;
                                break;
                            }
                        }
                    }
                    DoctorsOffice.doctors[doctorNum].acquire();
                    DoctorsOffice.numberOfFreeDoctors = DoctorsOffice.numberOfFreeDoctors - 1;
                    DoctorsOffice.freeDoctor.release();
                    this.visit(doctorNum);
                    DoctorsOffice.freeDoctor.acquire();
                    DoctorsOffice.numberOfFreeDoctors++;
                    DoctorsOffice.freeDoctor.release();
                }
            }
        } catch (InterruptedException ignored) {
        }
        // System.out.println(DoctorsOffice.patientsWhoLeft + DoctorsOffice.patientsWhoVisit);
        if (DoctorsOffice.numberOfPatients == DoctorsOffice.patientsWhoLeft + DoctorsOffice.patientsWhoVisit) {
            System.exit(0);
        }
    }

    public void visit(int numOfDoctor) throws InterruptedException {
        int t = DoctorsOffice.clock.time;
        int doctorsnum = numOfDoctor + 1;
        System.out.println(this.name + " visits doctor number " + doctorsnum + " at " + t + " .");
        while (DoctorsOffice.clock.time < t + 2) {
            synchronized (message) {
                message.wait();
            }
        }
        System.out.println(this.name + " leaves the office at " + DoctorsOffice.clock.time + " .");
        DoctorsOffice.patientsWhoVisit++;
    }
}
