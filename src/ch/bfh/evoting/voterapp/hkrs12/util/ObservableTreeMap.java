/* 
 * MobiVote
 * 
 *  MobiVote: Mobile application for boardroom voting
 *  Copyright (C) 2014 Bern
 *  University of Applied Sciences (BFH), Research Institute for Security
 *  in the Information Society (RISIS), E-Voting Group (EVG) Quellgasse 21,
 *  CH-2501 Biel, Switzerland
 * 
 *  Licensed under Dual License consisting of:
 *  1. GNU Affero General Public License (AGPL) v3
 *  and
 *  2. Commercial license
 * 
 *
 *  1. This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *
 *  2. Licensees holding valid commercial licenses for MobiVote may use this file in
 *   accordance with the commercial license agreement provided with the
 *   Software or, alternatively, in accordance with the terms contained in
 *   a written agreement between you and Bern University of Applied Sciences (BFH), 
 *   Research Institute for Security in the Information Society (RISIS), E-Voting Group (EVG)
 *   Quellgasse 21, CH-2501 Biel, Switzerland.
 * 
 *
 *   For further information contact us: http://e-voting.bfh.ch/
 * 
 *
 * Redistributions of files must retain the above copyright notice.
 */
package ch.bfh.evoting.voterapp.hkrs12.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;
import java.util.TreeMap;

/**
 * Map that can observed by another object which is notified when a change is done in the map
 * @author Philemon von Bergen
 * Inspired from different sources:
 * http://stackoverflow.com/questions/971927/should-i-add-support-for-propertychangesupport-and-propertychangelistener-in-a-j
 * http://code.google.com/p/i-gnoramus/source/browse/trunk/I-gnoramus/src/org/ignoramus/common/map/ObservableTreeMap.java?spec=svn99&r=99
 * 
 * @param <K> Key of the map entry
 * @param <V> Value contained in the map entry
 */
public class ObservableTreeMap<K, V> extends TreeMap<K, V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String PROP_PUT = "put";
	private PropertyChangeSupport propertySupport;

	public ObservableTreeMap() {
		super();
		propertySupport = new PropertyChangeSupport(this);
	}

	@Override
	public V put(K k, V v) {
		V old = super.put(k, v);
		propertySupport.firePropertyChange(PROP_PUT, old, v);
		return old;
	}
	
	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		super.putAll(map);
		propertySupport.firePropertyChange(PROP_PUT, null, null);
	}

	@Override
	public void clear() {
		super.clear();
		propertySupport.firePropertyChange(PROP_PUT, null, null);
	}

	@Override
	public V remove(Object key) {
		V value = super.remove(key);
		propertySupport.firePropertyChange(PROP_PUT, null, value);
		return value;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(listener);
	}

}
