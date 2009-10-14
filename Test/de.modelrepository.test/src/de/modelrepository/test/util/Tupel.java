package de.modelrepository.test.util;

public class Tupel<T,V> {
 private T o1;
 private V o2;

  public Tupel(T o1, V o2) {
    this.o1 = o1;
    this.o2 = o2;
  }

   public T getO1() {
     return o1;
   }

   public V getO2() {
     return o2;
   }
}