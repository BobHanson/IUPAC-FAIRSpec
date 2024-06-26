package com.integratedgraphics.ifd.vendor.bruker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.iupac.fairdata.api.IFDExtractorI;
import org.iupac.fairdata.spec.nmr.IFDNMRSpecData;
import org.iupac.fairdata.spec.nmr.IFDNMRSpecDataRepresentation;
import org.iupac.fairdata.util.Util;

import com.integratedgraphics.extractor.Extractor;
import com.integratedgraphics.ifd.api.IFDVendorPluginI;
import com.integratedgraphics.ifd.vendor.IFDDefaultVendorPlugin;

import jspecview.source.JDXReader;

public class BrukerIFDVendorPlugin extends IFDDefaultVendorPlugin {

	static {
		register(com.integratedgraphics.ifd.vendor.bruker.BrukerIFDVendorPlugin.class);
	}

	// public final static String defaultRezipIgnorePattern = "\\.mnova$";

	private final static Map<String, String> ifdMap = new HashMap<>();

	static {
		// order here is not significant; keys without the JCAMP vendor prefix are
		// derived, not the value itself
		String[] keys = { //
				"DIM", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_DIM, //
				"##$BF1", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_FREQ_1, //
				"##$BF2", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_FREQ_2, //
				"##$BF3", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_FREQ_3, //
				"##$NUC1", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_NUCL_1, //
				"##$NUC2", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_NUCL_2, //
				"##$NUC3", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_NUCL_3, //
				"##$PULPROG", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_PULSE_PROG, //
				"##$TE", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_TEMPERATURE_ABSOLUTE, //
				"SOLVENT", IFDNMRSpecData.IFD_PROP_SPEC_NMR_EXPT_SOLVENT, //
				"SF", IFDNMRSpecData.IFD_PROP_SPEC_NMR_INSTR_FREQ_NOMINAL, //
				"##$PROBHD", IFDNMRSpecData.IFD_PROP_SPEC_NMR_INSTR_PROBE_TYPE, //
		};
		for (int i = 0; i < keys.length;)
			ifdMap.put(keys[i++], keys[i++]);
	}

	/**
	 * 1D, 2D, ...; this value cannot be determined directly from parameters (I
	 * think)
	 */
	private String dim;

	public BrukerIFDVendorPlugin() {
		// files of interest; procs is just for solvent
		// presence of acqu2s indicates a 2D experiment
		paramRegex = "acqus$|acqu2s$|procs$";
		// rezip triggers for procs in a directory (1, 2, 3...) below a pdata directory,
		// such as pdata/1/procs. We do not add the "/" before pdata, because that could
		// be the| symbol, and that will be attached by IFDDefaultVendorPlugin in
		// super.getRezipRegex()
		rezipRegex = "pdata/[^/]+/procs$";
	}

	/**
	 * Require an unsigned integer, and if that is not there, replace the directory
	 * name with "1".
	 */
	@Override
	public String getRezipPrefix(String dirName) {
		return (isUnsignedInteger(dirName) ? null : "1");
	}

	/**
	 * .mnova files will be extracted by the mestrelab plugin. They should not be
	 * left in the Bruker dataset.
	 */

	@Override
	public boolean doRezipInclude(IFDExtractorI extractor, String baseName, String entryName) {
		String type = (entryName.endsWith(".pdf") ? IFDNMRSpecDataRepresentation.IFD_REP_SPEC_NMR_SPECTRUM_PDF
				: entryName.endsWith("thumb.png") ? IFDNMRSpecDataRepresentation.IFD_REP_SPEC_NMR_SPECTRUM_IMAGE
						: null);
		if (type != null)
			extractor.addProperty(type, Extractor.localizePath(baseName + entryName));
		return !entryName.endsWith(".mnova");
	}

	@Override
	public boolean doExtract(String entryName) {
		return false;
	}

	@Override
	public void startRezip(IFDExtractorI extractor) {
		// we will need dim for setting 1D
		super.startRezip(extractor);
		dim = null;
	}

	@Override
	public void endRezip() {
		// if we found an acqu2s file, then dim has been set to 2D already.
		// NUC2 will be set already, but that might just involve decoupling, which we
		// don't generally indicate. So here we remove the NUC2 property if this is a 1D
		// experiment.
		if (dim == null) {
			report("DIM", "1D");
			report("##$NUC2", null);
		}
		dim = null;
		super.endRezip();
	}

	/**
	 * We use jspecview.source.JDXReader (in Jmol.jar) to pull out the header as a
	 * map.
	 * 
	 */
	@Override
	public String accept(IFDExtractorI extractor, String ifdPath, byte[] bytes) {
		super.accept(extractor, ifdPath, bytes);
		return (readJDX(ifdPath, bytes) ? processRepresentation(ifdPath, null) : null);
	}

	private boolean readJDX(String ifdPath, byte[] bytes) {
		Map<String, String> map = null;
		try {
			map = JDXReader.getHeaderMap(new ByteArrayInputStream(bytes), null);
			// System.out.println(map.toString().replace(',', '\n'));
		} catch (Exception e) {
			// invalid format
			e.printStackTrace();
			return false;
		}
		if (ifdPath.indexOf("procs") >= 0) {
			report("SOLVENT", getSolvent(map));
			return true;
		}
		// no need to close a ByteArrayInputStream
		int ndim = 0;
		String nuc1;
		if ((nuc1 = processString(map, "##$NUC1", "off")) != null)
			ndim++;
		if ((processString(map, "##$NUC2", "off")) != null)
			ndim++;
		if (processString(map, "##$NUC3", "off") != null)
			ndim++;
		if (processString(map, "##$NUC4", "off") != null)
			ndim++;
		if (ndim == 0)
			return false;
		double freq1 = getDoubleValue(map, "##$BF1");
		if (ndim >= 2)
			getDoubleValue(map, "##$BF2");
		if (ndim >= 3)
			getDoubleValue(map, "##$BF3");
		if (ndim >= 4)
			getDoubleValue(map, "##$BF4");
		report("##$TE", getDoubleValue(map, "##$TE"));
		processString(map, "##$PULPROG", null);
		if (ifdPath.endsWith("acqu2s")) {
			report("DIM", dim = "2D");
		}
		report("SF", getNominalFrequency(freq1, nuc1));
		processString(map, "##$PROBHD", null);
		if (extractor != null)
			this.extractor = null;
		return true;
	}

	/**
	 * Looking here for &lt;nucl.solvent&gt; in ##$SREGLST
	 * 
	 * @param map
	 * @return "CDCl3" or "DMSO", for example
	 */
	private Object getSolvent(Map<String, String> map) {
		String nuc_solv = getBrukerString(map, "##$SREGLST");
		int pt;
		return (nuc_solv != null && (pt = nuc_solv.indexOf(".")) >= 0 ? nuc_solv.substring(pt + 1) : null);
	}

	/**
	 * Extract a Bruker &lt;xxxx&gt; string
	 * 
	 * @param map
	 * @param key
	 * @param ignore a value such as "off" to disregard
	 * @return null if not found or empty
	 */
	private String processString(Map<String, String> map, String key, String ignore) {
		String val = getBrukerString(map, key);
		if (val != null && !val.equals(ignore))
			report(key, val);
		return val;
	}

	/**
	 * remove &lt; and &gt; from the string
	 * 
	 * @param map
	 * @param key
	 * @return null if the value is not of the form &lt;xxx&gt; or is empty &lt;&gt;
	 */
	private static String getBrukerString(Map<String, String> map, String key) {
		return getDelimitedString(map, key, '<', '>');
	}

	/**
	 * Report the found property back to the IFDExtractorI class.
	 * 
	 * @param key
	 * @param val if null, this property is removed
	 */
	private void report(String key, Object val) {
		addProperty(ifdMap.get(key), val);
	}

////////// testing ///////////

	public static void main(String[] args) {
		test("test/cosy/acqus");
		test("test/cosy/acqu2s");
		test("test/cosy/procs");
		test("test/cosy/proc2s");
		test("test/13c/procs");
		test("test/13c/acqus");

	}

	private static void test(String ifdPath) {
		IFDVendorPluginI.init();
		System.out.println("====================" + ifdPath);
		try {
			String filename = new File(ifdPath).getAbsolutePath();
			byte[] bytes = Util.getLimitedStreamBytes(new FileInputStream(filename), -1, null, true, true);
			new BrukerIFDVendorPlugin().accept(null, filename, bytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getVendorName() {
		return "Bruker";
	}

	@Override
	public String processRepresentation(String ifdPath, byte[] bytes) {
		return IFDNMRSpecDataRepresentation.IFD_REP_SPEC_NMR_VENDOR_DATASET;
	}

}