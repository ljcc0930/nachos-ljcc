package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
//	System.out.println("\n ***Testing Boats with only 2 children***");
//	begin(0, 2, b);

	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 10 children, 10 adults***");
//  	begin(10, 10, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
	
		lock.acquire();
		for(int it = 0; it < children; it++) {
		    new KThread(new Runnable() {
			public void run() {
		            ChildItinerary();
		        }
		    }).setName("Child Boat Thread " + it).fork();
		}
		for(int it = 0; it < adults; it++) {
		    new KThread(new Runnable() {
			public void run() {
		            AdultItinerary();
		        }
		    }).setName("Adult Boat Thread " + it).fork();
		}
		mainSleep.sleep();
		lock.release();

    }

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
		lock.acquire();
		
		adultMembers++;
		
		while(true) {
			while(boatTarget || childMembers > 1 || onBoat > 0) {
				oAdult.sleep();
			}
		
			bg.AdultRowToMolokai();
			boatTarget = true;
			adultMembers--;
			if(!childArr) {
				bg.AdultRowToOahu();
				boatTarget = false;
				adultMembers++;
				oAdult.sleep();
				continue;
			}
			mChild.wake();
			break;
		}
	
		lock.release();
    }

    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
		boolean target = false;
		lock.acquire();
		
		childMembers++;
		while(true) {
			if(target) {
				while(!boatTarget) {
					mChild.sleep();
				}
				if(finished) {
					break;
				}
				bg.ChildRowToOahu();
				target = false;
				childMembers++;
				boatTarget = false;
				onBoat = 0;
				oAdult.wake();
				oChild.wake();
			}
			else {
				while(boatTarget || childMembers + onBoat < 2 || onBoat >= 2) {
					oChild.sleep();
				}
				if(onBoat == 0) {
					onBoat++;
					childMembers--;
					bg.ChildRowToMolokai();
					while(onBoat < 2) {
						oChild.wake();
						oBoat.sleep();
					}
					target = true;
					boatTarget = true;
					childArr = true;
					mChild.wake();
				}
				else {
					onBoat++;
					childMembers--;
					if(childMembers == 0 && adultMembers == 0) {
						finished = true;
						mainSleep.wake();
					}
					bg.ChildRideToMolokai();
					oBoat.wake();
					target = true;
				}
			}
		}
		
		lock.release();
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
    static Lock lock = new Lock();
    static boolean boatTarget = false;
    static boolean finished = false;
    static int childMembers;
    static int adultMembers;
    static boolean childArr = false;
    static Condition2 oChild = new Condition2(lock);
    static Condition2 mChild = new Condition2(lock);
    static Condition2 oAdult = new Condition2(lock);
    static Condition2 mainSleep = new Condition2(lock);
    static Condition2 oBoat = new Condition2(lock);
    static int onBoat;
    
}
