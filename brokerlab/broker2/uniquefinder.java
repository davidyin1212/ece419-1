import java.util.*;

public class uniquefinder {

public Vector<Integer> findunique(int [] arr1, int [] arr2){
   Vector<Integer> v = new Vector<Integer>(0,1);
   for(int i=0;i<arr1.length;i++){
     System.out.println("checking: "+arr1[i]);
     int element = arr1[i];
     int counter = 0;
     for(int j=0;j<arr2.length;j++){
       System.out.println("    checking: "+arr2[j]);
       if(element % arr2[j]==0){
          System.out.println(element +" divides into "+arr2[j]);
          counter++;
       }
     }
     //check to see if more than half of the array matched
     System.out.println("value of counter: "+counter);
     if(counter >= (arr2.length/2)){
         if(!v.contains(Integer.valueOf(arr1[i]))){
         v.addElement(new Integer(arr1[i]));
        }
     }
  }
  return v;
}

public static void main(String[] args) {
	int[] arr1 = {4,5,6,7,8};
	int[] arr2 = {2,3};
	
	uniquefinder u = new uniquefinder();
	
	Vector<Integer> v = u.findunique(arr1,arr2);
	System.out.println("length of vector is: "+v.size());
}

}


