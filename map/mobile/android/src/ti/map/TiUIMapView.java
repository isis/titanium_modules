/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.map;

import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIFragment;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

public class TiUIMapView extends TiUIFragment implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnCameraChangeListener, GoogleMap.OnInfoWindowClickListener
{

	private GoogleMap map;
	protected boolean animate = false;
	protected boolean preLayout = true;
	protected LatLngBounds preLayoutUpdateBounds;
	protected ArrayList<TiMarker> timarkers;
	protected AnnotationProxy selectedAnnotation;
	

	public TiUIMapView(final TiViewProxy proxy, Activity activity) {
		super(proxy, activity);
		timarkers = new ArrayList<TiMarker>();
	}

	@Override
	protected Fragment createFragment() {
		return SupportMapFragment.newInstance();
	}

	
	protected void processPreloadAnnotations() {
		ArrayList<AnnotationProxy> annotations = ((ViewProxy)proxy).getPreloadAnnotations();
		for (int i = 0; i < annotations.size(); i++) {
			addAnnotation(annotations.get(i));
		}
	}
	
	protected void processPreloadRoutes() {
		ArrayList<RouteProxy> routes = ((ViewProxy)proxy).getPreloadRoutes();
		for (int i = 0; i < routes.size(); i++) {
			addRoute(routes.get(i));
		}
	}
	
	protected void onViewCreated() {
		map = acquireMap();
		processMapProperties(proxy.getProperties());
		processPreloadAnnotations();
		processPreloadRoutes();
		map.setOnMarkerClickListener(this);
		map.setOnMapClickListener(this);
		map.setOnCameraChangeListener(this);
		map.setOnInfoWindowClickListener(this);
		((ViewProxy)proxy).clearPreloadObjects();
		proxy.fireEvent(TiC.EVENT_COMPLETE, null);
	}

	@Override
	public void processProperties(KrollDict d) {
		super.processProperties(d);

		if (acquireMap() == null) {
			return;
		}
		processMapProperties(d);
	}
	
	public void processMapProperties(KrollDict d) {

		if (d.containsKey(TiC.PROPERTY_USER_LOCATION)) {
			setUserLocation(d.getBoolean(TiC.PROPERTY_USER_LOCATION));
		}
		if (d.containsKey(TiC.PROPERTY_MAP_TYPE)) {
			setMapType(d.getInt(TiC.PROPERTY_MAP_TYPE));
		}
		if (d.containsKey(MapModule.PROPERTY_TRAFFIC)) {
			setTrafficEnabled(d.getBoolean(MapModule.PROPERTY_TRAFFIC));
		}
		if (d.containsKey(TiC.PROPERTY_ANIMATE)) {
			animate = d.getBoolean(TiC.PROPERTY_ANIMATE);
		}
		if (d.containsKey(TiC.PROPERTY_REGION)) {
			updateCamera(d.getKrollDict(TiC.PROPERTY_REGION));
		}
		if (d.containsKey(TiC.PROPERTY_ANNOTATIONS)) {
			Object[] annotations = (Object[]) d.get(TiC.PROPERTY_ANNOTATIONS);
			addAnnotations(annotations);
		}
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		
		if (key.equals(TiC.PROPERTY_USER_LOCATION)) {
			setUserLocation(TiConvert.toBoolean(newValue));
		}
		else if (key.equals(TiC.PROPERTY_MAP_TYPE)) {
			setMapType(TiConvert.toInt(newValue));
		}
		else if (key.equals(TiC.PROPERTY_REGION)) {
			updateCamera((HashMap) newValue);
		}
		else if (key.equals(MapModule.PROPERTY_TRAFFIC)) {
			setTrafficEnabled(TiConvert.toBoolean(newValue));
		}
		else if (key.equals(TiC.PROPERTY_ANIMATE)) {
			animate = TiConvert.toBoolean(newValue);
		}
		else if (key.equals(TiC.PROPERTY_ANNOTATIONS)) {
			updateAnnotations((Object[]) newValue);
		} 
		else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}

	}

	public GoogleMap acquireMap() {
		return ((SupportMapFragment) getFragment()).getMap();
	}
	
	public GoogleMap getMap() {
		return map;
	}

	protected void setUserLocation(boolean enabled) {
		map.setMyLocationEnabled(enabled);
	}
	
	protected void setMapType(int type) {
		map.setMapType(type);
	}
	
	protected void setTrafficEnabled(boolean enabled) {
		map.setTrafficEnabled(enabled);
	}
	
	protected void updateCamera(HashMap<String, Object> dict) {
		double longitude = 0;
		double longitudeDelta = 0;
		double latitude = 0;
		double latitudeDelta = 0;

		if (dict.containsKey(TiC.PROPERTY_LATITUDE)) {
			latitude = TiConvert.toDouble(dict, TiC.PROPERTY_LATITUDE);
		}
		if (dict.containsKey(TiC.PROPERTY_LONGITUDE)) {
			longitude = TiConvert.toDouble(dict, TiC.PROPERTY_LONGITUDE);
		}
		
		CameraPosition.Builder cameraBuilder = new CameraPosition.Builder();
		LatLng location = new LatLng(latitude, longitude);
		cameraBuilder.target(location);
		
		if (dict.containsKey(TiC.PROPERTY_LATITUDE_DELTA)) {
			latitudeDelta = TiConvert.toDouble(dict, TiC.PROPERTY_LATITUDE_DELTA);
		}

		if (dict.containsKey(TiC.PROPERTY_LONGITUDE_DELTA)) {
			longitudeDelta = TiConvert.toDouble(dict, TiC.PROPERTY_LONGITUDE_DELTA);
		}
	 
		if (latitudeDelta != 0 && longitudeDelta != 0) {
			LatLng northeast = new LatLng(latitude + (latitudeDelta / 2.0), longitude + (longitudeDelta / 2.0));
			LatLng southwest = new LatLng(latitude - (latitudeDelta / 2.0), longitude - (longitudeDelta / 2.0));

			final LatLngBounds bounds = new LatLngBounds(southwest, northeast);
			if (preLayout) {
				preLayoutUpdateBounds = bounds;
				return;
			} else {
				moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
				return;
			}
		}
		
		CameraPosition position = cameraBuilder.build();
		CameraUpdate camUpdate = CameraUpdateFactory.newCameraPosition(position);
		moveCamera(camUpdate);
	}
	
	protected void moveCamera(CameraUpdate camUpdate)
	{
		if (animate) {
			map.animateCamera(camUpdate);
		} else {
			map.moveCamera(camUpdate);
		}
	}
	
	protected void addAnnotation(AnnotationProxy annotation) {
		//if annotation already on map, remove it first then re-add it
		TiMarker tiMarker = annotation.getTiMarker();
		if (tiMarker != null) {
			removeAnnotation(tiMarker);
		}
		annotation.processOptions();
		//add annotation to map view
		Marker marker = map.addMarker(annotation.getMarkerOptions());
		tiMarker = new TiMarker(marker, annotation);
		annotation.setTiMarker(tiMarker);
		timarkers.add(tiMarker);
	}

	protected void addAnnotations(Object[] annotations) 
	{
		for (int i = 0; i < annotations.length; i++) {
			Object obj = annotations[i];
			if (obj instanceof AnnotationProxy) {
				AnnotationProxy annotation = (AnnotationProxy) obj;
				addAnnotation(annotation);
			}
		}
	}
	
	protected void updateAnnotations(Object[] annotations)
	{
		//First, remove old annotations from map
		removeAllAnnotations();
		//Then we add new annotations to the map
		addAnnotations(annotations);
	}
	
	protected void removeAllAnnotations() {
		for (int i = 0; i < timarkers.size(); i++) {
			TiMarker timarker = timarkers.get(i);
			timarker.getMarker().remove();
			AnnotationProxy proxy = timarker.getProxy();
			if (proxy != null) {
				proxy.setTiMarker(null);
			}
		}
		timarkers.clear();
	}
	
	public TiMarker findMarkerByTitle(String title) {
		for (int i = 0; i < timarkers.size(); i++) {
			TiMarker timarker = timarkers.get(i);
			if (timarker.getMarker().getTitle().equals(title)) {
				return timarker;
			}
		}
		
		return null;
	}

	protected void removeAnnotation(Object annotation) {
		TiMarker timarker = null;
		if (annotation instanceof TiMarker) {
			timarker = (TiMarker)annotation;
		} else if (annotation instanceof AnnotationProxy) {
			timarker = ((AnnotationProxy)annotation).getTiMarker();
		} else if (annotation instanceof String) {
			timarker = findMarkerByTitle((String)annotation);
		}
		
		if (timarker != null && timarkers.remove(timarker)) {
			timarker.getMarker().remove();
			AnnotationProxy proxy = timarker.getProxy();
			if (proxy != null) {
				proxy.setTiMarker(null);
			}
		}
	}
	
	protected void selectAnnotation(Object annotation) {
		if (annotation instanceof AnnotationProxy) {
			AnnotationProxy proxy = (AnnotationProxy)annotation;
			if (proxy.getTiMarker() != null) {
				proxy.showInfo();
				selectedAnnotation = proxy;
			}
		} else if (annotation instanceof String) {
			String title = (String) annotation;
			TiMarker marker = findMarkerByTitle(title);
			if (marker != null) {
				marker.getMarker().showInfoWindow();
				selectedAnnotation = marker.getProxy();
			}

		}
	}
	
	protected void deselectAnnotation(Object annotation) {
		if (annotation instanceof AnnotationProxy) {
			AnnotationProxy proxy = (AnnotationProxy)annotation;
			if (proxy.getTiMarker() != null) {
				((AnnotationProxy)annotation).hideInfo();
			}
		} else if (annotation instanceof String) {
			String title = (String) annotation;
			TiMarker marker = findMarkerByTitle(title);
			if (marker != null) {
				marker.getMarker().hideInfoWindow();
			}
		}
		selectedAnnotation = null;
	}

	private AnnotationProxy getProxyByMarker(Marker m) {
		for (int i = 0; i < timarkers.size(); i++) {
			TiMarker timarker = timarkers.get(i);
			if (m.equals(timarker.getMarker())){
				return timarker.getProxy();
			}
		}
		return null;
	}
	
	public void addRoute(RouteProxy r) {
		//check if route already added.
		if (r.getRoute() != null) {
			return;
		}

		r.processOptions();
		r.setRoute(map.addPolyline(r.getOptions()));
	}
	
	public void removeRoute (RouteProxy r) {
		if (r.getRoute() == null) {
			return;
		}
		
		r.getRoute().remove();
		r.setRoute(null);
	}

	public void fireClickEvent(Marker marker, AnnotationProxy annoProxy, Object clickSource) {
		KrollDict d = new KrollDict();
		d.put(TiC.PROPERTY_TITLE, marker.getTitle());
		d.put(TiC.PROPERTY_SUBTITLE, marker.getSnippet());
		d.put(TiC.PROPERTY_LATITUDE, marker.getPosition().latitude);
		d.put(TiC.PROPERTY_LONGITUDE, marker.getPosition().longitude);
		d.put(TiC.PROPERTY_ANNOTATION, annoProxy);
		d.put(MapModule.PROPERTY_MAP, proxy);
		d.put(TiC.PROPERTY_TYPE, TiC.EVENT_CLICK);
		d.put(TiC.PROPERTY_SOURCE, proxy);
		d.put(TiC.EVENT_PROPERTY_CLICKSOURCE, clickSource);
		proxy.fireEvent(TiC.EVENT_CLICK, d);
	}
	public void fireInfoWindowClickEvent(Marker marker, AnnotationProxy annoProxy, Object clickSource) {
		KrollDict d = new KrollDict();
		d.put(TiC.PROPERTY_TITLE, marker.getTitle());
		d.put(TiC.PROPERTY_SUBTITLE, marker.getSnippet());
		d.put(TiC.PROPERTY_LATITUDE, marker.getPosition().latitude);
		d.put(TiC.PROPERTY_LONGITUDE, marker.getPosition().longitude);
		d.put(TiC.PROPERTY_ANNOTATION, annoProxy);
		d.put(MapModule.PROPERTY_MAP, proxy);
		d.put(TiC.PROPERTY_TYPE, TiC.EVENT_CLICK);
		d.put(TiC.PROPERTY_SOURCE, proxy);
		d.put(TiC.EVENT_PROPERTY_CLICKSOURCE, clickSource);
		proxy.fireEvent("infowindowclick", d);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		AnnotationProxy annoProxy = getProxyByMarker(marker);
		if (selectedAnnotation == null) {
			annoProxy.showInfo();
			selectedAnnotation = annoProxy;
		} else if (!selectedAnnotation.equals(annoProxy)) {
			selectedAnnotation.hideInfo();
			annoProxy.showInfo();
			selectedAnnotation = annoProxy;
		} else {
			selectedAnnotation.hideInfo();
			selectedAnnotation = null;
		}
		fireClickEvent(marker, annoProxy, annoProxy);
		return true;
	}

	@Override
	public void onMapClick(LatLng point) {
		if (selectedAnnotation != null) {
			TiMarker tiMarker = selectedAnnotation.getTiMarker();
			if (tiMarker != null) {
				fireClickEvent(tiMarker.getMarker(), selectedAnnotation, null);
			}
			selectedAnnotation = null;
		}
		
	}
	
	@Override
    public void onInfoWindowClick (Marker marker) {
		AnnotationProxy annoProxy = getProxyByMarker(marker);
		/*if (selectedAnnotation == null) {
			annoProxy.showInfo();
			selectedAnnotation = annoProxy;
		} else if (!selectedAnnotation.equals(annoProxy)) {
			selectedAnnotation.hideInfo();
			annoProxy.showInfo();
			selectedAnnotation = annoProxy;
		} else {
			selectedAnnotation.hideInfo();
			selectedAnnotation = null;
		}*/
		fireInfoWindowClickEvent(marker, annoProxy, annoProxy);
    }	
    
	@Override
	public void release() {
		selectedAnnotation = null;
		map.clear();
		map = null;
		timarkers.clear();
	}

	@Override
	public void onCameraChange(CameraPosition position) {
		if (preLayout) {
			if (preLayoutUpdateBounds != null) {
				moveCamera(CameraUpdateFactory.newLatLngBounds(preLayoutUpdateBounds, 0));
				preLayoutUpdateBounds = null;
			} else {
				//moveCamera will trigger another callback, so we do this to make sure
				//we don't fire event when region is set initially
				preLayout = false;
			}
		} else if (proxy != null) {
			KrollDict d = new KrollDict();
			d.put(TiC.PROPERTY_LATITUDE, position.target.latitude);
			d.put(TiC.PROPERTY_LONGITUDE, position.target.longitude);
			d.put("zoom", position.zoom);
			d.put("tilt", position.tilt);
			d.put("bearing", position.bearing);
			d.put(TiC.PROPERTY_SOURCE, proxy);
			proxy.fireEvent(TiC.EVENT_REGION_CHANGED, d);
		}
		
	}

}