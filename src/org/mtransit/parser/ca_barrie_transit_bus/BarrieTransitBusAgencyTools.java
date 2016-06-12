package org.mtransit.parser.ca_barrie_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://www.barrie.ca/Living/Getting%20Around/BarrieTransit/Pages/Barrie-GTFS.aspx
// http://transit.cityofbarriesites.com/files/google_transit.zip
public class BarrieTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-barrie-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new BarrieTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating Barrie Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating Barrie Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String A = "A";
	private static final String B = "B";

	@Override
	public long getRouteId(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		matcher.find();
		return Long.parseLong(matcher.group());
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		matcher.find();
		return matcher.group();
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_BLUE = "336699"; // BLUE (from web site CSS)
	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_EC008C = "EC008C";
	private static final String COLOR_ED1C24 = "ED1C24";
	private static final String COLOR_0089CF = "0089CF";
	private static final String COLOR_918BC3 = "918BC3";
	private static final String COLOR_8ED8F8 = "8ED8F8";
	private static final String COLOR_B2D235 = "B2D235";
	private static final String COLOR_F58220 = "F58220";
	private static final String COLOR_000000 = "000000";
	private static final String COLOR_007236 = "007236";
	private static final String COLOR_FFFF00 = "FFFF00";

	@Override
	public String getRouteColor(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		matcher.find();
		int routeId = Integer.parseInt(matcher.group());
		switch (routeId) {
		// @formatter:off
		case 1: return COLOR_EC008C;
		case 2: return COLOR_ED1C24;
		case 3: return COLOR_0089CF;
		case 4: return COLOR_918BC3;
		case 5: return COLOR_8ED8F8;
		case 6: return COLOR_B2D235;
		case 7: return COLOR_F58220;
		case 8: return COLOR_000000;
		case 11: return COLOR_FFFF00;
		case 90: return COLOR_007236;
		// @formatter:on
		}
		System.out.printf("\nUnexpected route color %s!", gRoute);
		System.exit(-1);
		return null;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(11l, new RouteTripSpec(11l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Fenchurch Mnr", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Pk Pl") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1", "247", "775", "776" //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"776", "775", "160", "1" //
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		GRoute gRoute = gtfs.getRoute(gTrip.getRouteId());
		String rsn = gRoute.getRouteShortName();
		String rsn_letter = rsn.substring(rsn.length() - 1, rsn.length());
		String tripHeadsign = rsn_letter + " " + getRouteLongName(gRoute);
		int directionId;
		if (A.equals(rsn_letter)) {
			directionId = 0;
		} else if (B.equals(rsn_letter)) {
			directionId = 1;
		} else {
			System.out.printf("\nUnexpected trip (unexpected rsn: %s): %s", rsn, gTrip);
			System.exit(-1);
			directionId = -1;
		}
		mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), directionId);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}



	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.parseInt(gStop.getStopCode()); // use stop code as stop ID
	}
}
