package org.opencloudb.mpp.tmp;

import org.opencloudb.mpp.OrderCol;
import org.opencloudb.mpp.RowDataPacketSorter;
import org.opencloudb.net.mysql.RowDataPacket;

import java.util.*;

/**
 * 
 * @author coderczp-2014-12-8
 */
public class RowDataSorter extends RowDataPacketSorter {

    //记录总数(=offset+limit)
    private volatile int total;
    //查询的记录数(=limit)
    private volatile int size;
    //堆
    private volatile HeapItf heap;
    //多列比较器
    private volatile RowDataCmp cmp;
    //是否执行过buildHeap
    private volatile boolean hasBuild;

    public RowDataSorter(OrderCol[] orderCols) {
        super(orderCols);
        this.cmp = new RowDataCmp(orderCols);
    }

    public synchronized void setLimit(int start, int size) {
        //容错处理
        if (start < 0) {
            start = 0;
        }
        if (size <= 0) {
            this.total = this.size = Integer.MAX_VALUE;
        } else {
            this.total = start + size;
            this.size = size;
        }
        //统一采用顺序，order by 条件交给比较器去处理
        this.heap = new MaxHeap(cmp, total);
    }

    @Override
    public synchronized void addRow(RowDataPacket row) {
        if (heap.getData().size() < total) {
            heap.add(row);
            return;
        }
        //堆已满，构建最大堆，并执行淘汰元素逻辑
        if (heap.getData().size() == total && hasBuild == false) {
            heap.buildHeap();
            hasBuild = true;
        }
        heap.addIfRequired(row);
    }

    @Override
    public Collection<RowDataPacket> getSortedResult() {
        final List<RowDataPacket> data = heap.getData();
        int size = data.size();
        if (size < 2) {
            return data;
        } else {
            //构建最大堆并排序
            if (!hasBuild) {
                heap.buildHeap();
            }
            heap.heapSort(this.size);
            return heap.getData();
        }
    }
}
