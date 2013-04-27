/*
 * BlockList an alternative java.util.List
 * Copyright 2011-2013 MeBigFatGuy.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.blocklist;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@SuppressWarnings("unchecked")
/**
 * an implementation of List that uses blocks of arrays to store items.
 * The data structure is basically E[][], where E[] represents a list of 
 * blocks, and the leaf arrays hold the data. The leaf arrays use
 * index 0 as a special element where the 'next empty position' is stored, for
 * that leaf array. This value is stored as an Integer. Thus the real data is stored at 
 * index 1 -> emptyPos in the leaf arrays.
 * 
 * The advantage to this List implementation is inserts anywhere never require the
 * reallocation of the entire list, nor the re-shuffling of the entire list.
 * The modifications are limited to moving the block array, and perhaps reallocating 
 * two leaf blocks.
 * 
 * Since the data is held in sub-blocks, there is no massive array allocated, making large lists
 * easier to allocate in memory.
 * 
 * This list is not thread safe.
 */
public class BlockList<E> implements List<E>, Externalizable {

	private static final long serialVersionUID = -2221663525758235084L;
	public static final int DEFAULT_BLOCK_COUNT = 1;
	public static final int DEFAULT_BLOCK_SIZE = 32;

	private E[][] blocks;
	private int blockSize;
	private int size;
	private int revision;

	public BlockList() {
		this(DEFAULT_BLOCK_SIZE);
	}

	public BlockList(int blockSize) {
		this(DEFAULT_BLOCK_COUNT, blockSize);
	}

	public BlockList(int initialBlkCount, int blkSize) {
		blocks = (E[][])new Object[initialBlkCount][];
		blockSize = blkSize;
		size = 0;
		for (int b = 0; b < blocks.length; b++) {
			blocks[b] = (E[])new Object[1 + blockSize];
			blocks[b][0] = (E)Integer.valueOf(0);
		}
		revision = 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BlockList) {
			BlockList<E> that = (BlockList<E>) o;
			if (this.size != that.size) {
				return false;
			}

			for (int i = 0; i < size; i++) {
				Object thisItem = get(i);
				Object thatItem = that.get(i);

				if (thisItem == null) {
					if (thatItem != null) {
						return false;
					}
				}

				if (thatItem == null) {
					return false;
				}

				if (!thisItem.equals(thatItem)) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		return size;
	}

	@Override
	public boolean add(E element) {
		long blockPtr = findBlock(size, true);
		if (blockPtr < 0) {
			grow();
			blockPtr = ((long)(blocks.length - 1)) << 32;
		}

		int blkIndex = (int)(blockPtr >> 32);
		E[] blk = blocks[blkIndex];
		int emptyPos = ((Integer)blk[0]).intValue();
		blk[1 + emptyPos++] = element;
		blk[0] = (E)Integer.valueOf(emptyPos);
		size++;
		revision++;
		return true;
	}

	@Override
	public void add(int index, E element) {
		long blockPtr = findBlock(index, true);
		if (blockPtr < 0) {
			grow();
			blockPtr = ((long)(blocks.length - 1)) << 32;
		}

		int blkIndex = (int)(blockPtr >> 32);
		int blkOffset = (int)blockPtr;

		E[] blk = blocks[blkIndex];
		int emptyPos = ((Integer) blk[0]).intValue();
		if (emptyPos == blockSize){
			splitBlock(blkIndex, blkOffset);
			blk = blocks[blkIndex];
		} else if (blkOffset < emptyPos) {
			System.arraycopy(blk, 1 + blkOffset, blk, 1 + blkOffset + 1, emptyPos - blkOffset);
		}

		blk[1 + blkOffset] = element;
		emptyPos = ((Integer) blk[0]).intValue();
		blk[0] = (E) Integer.valueOf(emptyPos + 1);
		size++;
		revision++;
	}

	@Override
	public boolean addAll(Collection<? extends E> elements) {
		for (E e : elements) {
			add(e);
		}
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> elements) {
		for (E e : elements) {
			add(index++, e);
		}
		return true;
	}

	@Override
	public void clear() {
		blocks = (E[][]) new Object[0][];
		size = 0;
		for (int b = 0; b < blocks.length; b++) {
			blocks[b] = (E[]) new Object[1 + blockSize];
			blocks[b][0] = (E) Integer.valueOf(0);
		}
		revision++;
	}

	@Override
	public boolean contains(Object element) {
		if (element == null) {
			return false;
		}

		for (E[] blk : (E[][]) blocks) {
		    int emptyPos = ((Integer) blk[0]).intValue();
			for (int s = 0; s < emptyPos; s++) {
				if (element.equals(blk[1+s])) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean containsAll(Collection<?> elements) {
		for (Object o : elements) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public E get(int index) {
		long blockPtr = findBlock(index, false);
		if (blockPtr < 0) {
			throw new IndexOutOfBoundsException("Index (" + index + ") is out of bounds [0 <= i < " + size + "]");
		}

		int blkIndex = (int)(blockPtr >> 32);
		int blkOffset = (int)blockPtr;

		E[] blk = blocks[blkIndex];
		return blk[1+blkOffset];
	}

	@Override
	public int indexOf(Object element) {
		if (element == null) {
			return -1;
		}

		int pos = 0;
		for (E[] blk : blocks) {
		    int emptyPos = ((Integer) blk[0]).intValue();
			for (int s = 0; s < emptyPos; s++) {
				if (element.equals(blk[1+s])) {
					return pos;
				}
				pos++;
			}
		}

		return -1;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public Iterator<E> iterator() {
		return new BlockListIterator();
	}

	@Override
	public int lastIndexOf(Object element) {
		if (element == null) {
			return -1;
		}

		int pos = size - 1;
		for (int b = blocks.length - 1; b >= 0; b--) {
			E[] blk = blocks[b];
			int emptyPos = ((Integer) blk[0]).intValue();
			for (int s = emptyPos-1; s>= 0; s--) {
				if (element.equals(blk[1+s])) {
					return pos;
				}
				pos--;
			}
		}

		return -1;
	}

	@Override
	public ListIterator<E> listIterator() {
		throw new UnsupportedOperationException("listIterator");
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		throw new UnsupportedOperationException("listIterator");
	}

	@Override
	public boolean remove(Object element) {
		int pos = indexOf(element);
		if (pos < 0) {
			revision++;
			return false;
		}

		remove(pos);
		return true;
	}

	@Override
	public E remove(int index) {
		long blockPtr = findBlock(index, false);
		if (blockPtr < 0) {
			revision++;
			throw new IndexOutOfBoundsException("Index (" + index + ") is out of bounds [0 <= i < " + size + "]");
		}

		int blkIndex = (int)(blockPtr >> 32);
		int blkOffset = (int)blockPtr;

		return remove(blkIndex, blkOffset);
	}

	protected E remove(int blkIndex, int blkOffset) {
		E[] blk = blocks[blkIndex];
		E e = blk[1+blkOffset];
		int emptyPos = ((Integer) blk[0]).intValue();
		if (emptyPos == 1) {
			System.arraycopy(blocks, blkIndex+1, blocks, blkIndex, blocks.length - blkIndex - 1);
			blocks[blocks.length - 1] = blk;
		} else {
			System.arraycopy(blk, 1+blkOffset + 1, blk, 1+blkOffset, emptyPos - blkOffset - 1);

		}
		blk[1+emptyPos-1] = null;
		blk[0] = (E) Integer.valueOf(emptyPos-1);
		size--;
		revision++;
		return e;
	}

	@Override
	public boolean removeAll(Collection<?> elements) {
		boolean removed = false;
		for (Object e : elements) {
			removed |= remove(e);
		}
		return removed;
	}

	@Override
	public boolean retainAll(Collection<?> elements) {
		boolean changed = false;

		int pos = 0;
		for (int b = 0; b < blocks.length; b++) {
			E[] blk = blocks[b];
			int emptyPos = ((Integer) blk[0]).intValue();
			for (int s = 0; s < emptyPos; s++) {

				if (!elements.contains(blk[1+s])) {
					boolean blockRemoved = (emptyPos == 1);
					remove(pos);
					changed = true;
					s--;
					emptyPos--;
					if (blockRemoved) {
						b--;
					}
				} else {
					pos++;
				}
			}
		}

		revision++;
		return changed;
	}

	@Override
	public E set(int index, E element) {
		long blockPtr = findBlock(index, false);
		if (blockPtr < 0) {
			throw new IndexOutOfBoundsException("Index (" + index + ") is out of bounds [0 <= i < " + size + "]");
		}

		int blkIndex = (int)(blockPtr >> 32);
		int blkOffset = (int)blockPtr;

		E[] blk = blocks[blkIndex];
		E oldValue = blk[1+blkOffset];
		blk[1+blkOffset] = element;
		return oldValue;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public List<E> subList(int index, int length) {
		throw new UnsupportedOperationException("BlockList.subList not implemented yet");
	}

	@Override
	public Object[] toArray() {
		Object[] o = new Object[size];
		int pos = 0;
		for (E[] blk : blocks) {
		    int emptyPos = ((Integer) blk[0]).intValue();
			System.arraycopy(blk, 1+0, o, 1+pos, emptyPos);
			pos += emptyPos;
		}
		return o;
	}

	@Override
	public <AE> AE[] toArray(AE[] proto) {
		if (proto.length < size) {
			Class<?> cls = proto.getClass().getComponentType();
			proto = (AE[])Array.newInstance(cls, size);
		}

		int pos = 0;
		for (E[] blk : blocks) {
		    int emptyPos = ((Integer) blk[0]).intValue();
			System.arraycopy(blk, 1+0, proto, pos, emptyPos);
			pos += emptyPos;
		}
		return proto;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(size * 10);
		String cr = "\n";
		String sep = "";
		String comma = "";
		for (E[] blk : blocks) {
	        sb.append(sep);
		    int emptyPos = ((Integer) blk[0]).intValue();
		    for (int i = 0; i < emptyPos; ++i) {
		        sb.append(comma);
		        sb.append(blk[1+i]);
		        comma = ",";
		    }
			sep = cr;
		}
		return sb.toString();
	}

	private long findBlock(int index, boolean forAdd) {
		int offset = 0;

		if (index < (size / 2)) {
    		for (int b = 0; b < blocks.length; ++b) {
    			E[] blk = blocks[b];
    			int emptyPos = ((Integer) blk[0]).intValue();
    			int nextOffset = offset + emptyPos;
    			if ((index < nextOffset) || (forAdd && ((index == nextOffset) && (emptyPos < blockSize)))) {
    				return (((long) b) << 32) | (index - offset);
    			}
    			offset = nextOffset;
    		}
		} else {
		    offset = size;
		    for (int b = blocks.length-1; b >=0; --b) {
		        E[] blk = blocks[b];
		        int emptyPos = ((Integer) blk[0]).intValue();
		        int nextOffset = offset - emptyPos;
		        if (((index >= nextOffset) && (index < offset)) || (forAdd && ((index == offset) && (emptyPos < blockSize)))) {
		            return (((long) b) << 32) | (index - nextOffset);
                }
                offset = nextOffset;
		    }
		}
		return -1L;
	}

	private void grow() {
		E[][] newBlocks = (E[][]) new Object[blocks.length+1][];
		System.arraycopy(blocks, 0, newBlocks, 0, blocks.length);
		newBlocks[blocks.length] = (E[]) new Object[1+blockSize];
		newBlocks[blocks.length][0] = (E) Integer.valueOf(0);
		blocks = newBlocks;
	}

	private void splitBlock(int blockIndex, int blockOffset) {
		E[][] newBlocks = (E[][]) new Object[blocks.length+1][];
		System.arraycopy(blocks, 0, newBlocks, 0, blockIndex);
		System.arraycopy(blocks, blockIndex, newBlocks, blockIndex + 1, blocks.length - blockIndex);
		
		newBlocks[blockIndex] = (E[]) new Object[1+blockSize];
        int emptyPos = ((Integer) blocks[blockIndex][0]).intValue();
		if (blockOffset != 0) {
		    System.arraycopy(blocks[blockIndex], 1+0, newBlocks[blockIndex], 1+0, blockOffset);
		    System.arraycopy(blocks[blockIndex], 1+blockOffset, newBlocks[blockIndex+1], 1+0, emptyPos - blockOffset);
		    Arrays.fill(newBlocks[blockIndex + 1], 1+blockSize - blockOffset, 1+blockSize, null);
		}
		newBlocks[blockIndex+1][0] = (E) Integer.valueOf(emptyPos - blockOffset);
		newBlocks[blockIndex][0] = (E) Integer.valueOf(blockOffset);
		blocks = newBlocks;
	}

	private class BlockListIterator implements Iterator<E> {

		private int pos = 0;
		private int iteratorRevision = revision;


		@Override
		public boolean hasNext() {
			if (revision != iteratorRevision) {
				throw new ConcurrentModificationException();
			}

			return pos < size;
		}

		@Override
		public E next() {
			if (revision != iteratorRevision) {
				throw new ConcurrentModificationException();
			}

			if (pos >= size) {
				throw new IndexOutOfBoundsException("Index (" + pos + ") is out of bounds [0 <= i < " + size + "]");
			}

			return get(pos++);
		}

		@Override
		public void remove() {
			if (revision != iteratorRevision) {
				throw new ConcurrentModificationException();
			}

			BlockList.this.remove(pos);
			iteratorRevision = revision;
		}

	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(blockSize);
		out.writeInt(size);

		for (E[] blk : blocks) {
		    int emptyPos = ((Integer) blk[0]).intValue();
			for (int s = 0; s < 1+emptyPos; s++) {
				out.writeObject(blk[s]);
			}
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {

		blockSize = in.readInt();
		size = in.readInt();

		int numBlocks = (size + (blockSize - 1)) / blockSize;
		if (numBlocks > DEFAULT_BLOCK_COUNT) {
			blocks = (E[][]) new Object[numBlocks][];
		} else {
		    blocks = (E[][]) new Object[DEFAULT_BLOCK_COUNT][];
		}
		
		for (int i = 0; i < numBlocks; i++) {
		    blocks[i] = (E[]) new Object[1+blockSize];
		    blocks[i][0] = (E) Integer.valueOf(0);
		}

		for (int i = 0; i < numBlocks; i++) {
		    E[] blk = blocks[i];
		    blk[0] = (E) in.readObject();
		    
		    int emptyPos = ((Integer) blk[0]).intValue();
		    for (int s = 0; s < emptyPos; s++) {
		        blk[1+s] = (E) in.readObject();
		    }
		}
	}
}
