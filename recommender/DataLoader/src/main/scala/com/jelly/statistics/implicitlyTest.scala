package com.jelly.statistics

object implicitlyTest {
  // 泛型Ordering，隐式类型T。主构造器，val first: T, val second: T
  class PairImplicits[T: Ordering](val first: T, val second: T) {
    // ordered隐式值。 比较大小，返回结果
    def bigger(implicit ordered: Ordering[T]) =
      if (ordered.compare(first, second) > 0) first else second
  }

  class PairImplicitly[T: Ordering](val first: T, val second: T) {
    // implicitly关键字：提取运行时上下文件界定时的实例值。
    def bigger = if (implicitly[Ordering[T]].compare(first, second) > 0) first else second
  }

  class PairImplicitlyOdereded[T: Ordering](val first: T, val second: T) {
    def bigger = {
      // 引用Ordered下所有的累
      import Ordered._
      // >函数，使用隐式转换成Ordered类型，进行比较
      if(first >  second) first else second
    }
  }


    def main(args: Array[String]): Unit = {
      // 执行结果：9
      println(new PairImplicits(7,9).bigger);
      // 执行结果：9
      println(new PairImplicitly(7,9).bigger);
      // 执行结果：9
      println(new PairImplicitlyOdereded(7,9).bigger);
    }

}
