package org.iupac.fairspec.util;

import java.util.HashMap;
import java.util.Map;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSPropertyManagerI;
import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.core.IFSStructureRepresentation;
import org.jmol.api.JmolViewer;
import org.jmol.viewer.Viewer;

import javajs.util.BS;

public class IFSDefaultStructurePropertyManager implements IFSPropertyManagerI {

	/**
	 * the associated extractor
	 */
	private IFSExtractorI extractor;

	private Viewer jmolViewer;

	/**
	 * A class to process structures using Jmol methods to extract and discover
	 * properties of a model.
	 * 
	 */

	public IFSDefaultStructurePropertyManager(IFSExtractorI extractor) {
		this.extractor = extractor;
	}

	@Override
	public String getParamRegex() {
		return "(?<struc>(?<mol>\\.mol$|\\.sdf$)|(?<cdx>\\.cdx$|\\.cdxml$))";
	}

	@Override
	public boolean doExtract(String entryName) {
		return true;
	}

	private Map<String, String> fileToType = new HashMap<>();

	@Override
	public boolean accept(IFSExtractorI extractor, String fname, byte[] bytes) {
		fileToType.put(fname, getType(fname, bytes));
		return true;
	}

	private String getType(String fname, byte[] bytes) {
		String ext = fname.substring(fname.lastIndexOf('.') + 1);
		String type = fileToType.get(fname);
		if (type != null)
			return type;
		boolean check2d = false;
		switch (ext) {
		case "mol":
			type = IFSStructureRepresentation.IFS_STRUCTURE_REP_MOL;
			check2d = true;
			break;
		case "sdf":
			type = IFSStructureRepresentation.IFS_STRUCTURE_REP_SDF;
			check2d = true;
			break;
		case "cdf":
			type = IFSStructureRepresentation.IFS_STRUCTURE_REP_CDF;
			break;
		case "cdxml":
			type = IFSStructureRepresentation.IFS_STRUCTURE_REP_CDXML;
			break;
		default:
			type = ext.toUpperCase();
			break;
		}

		boolean is2D = false;
		String smiles = null, inchi = null, inchiKey = null;

		if (check2d) {
			try {
				Viewer v = getJmolViewer();
				v.loadInline(new String(bytes));
				BS atoms = v.bsA();

				smiles = v.getSmiles(atoms);
				inchi = v.getInchi(atoms, null, null);
				inchiKey = v.getInchi(atoms, null, "key");

				// check 2D or 3D
				Map<String, Object> info = v.getCurrentModelAuxInfo();
				is2D = "2D".equals(info.get("dimension"));
				if (is2D) {
					type += ".2d";
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// .getFileType(Rdr.getBufferedReader(Rdr.getBIS(bytes), null));
		}
		if (smiles != null) {
			IFSPropertyManagerI.addProperty(extractor, IFSConst.IFS_STRUCTURE_PROP_SMILES, smiles);
		}
		if (inchi != null) {
			IFSPropertyManagerI.addProperty(extractor, IFSConst.IFS_STRUCTURE_PROP_INCHI, inchi);
		}
		if (inchiKey != null) {
			IFSPropertyManagerI.addProperty(extractor, IFSConst.IFS_STRUCTURE_PROP_INCHIKEY, inchiKey);
		}
		return type;
	}

	protected Viewer getJmolViewer() {
		if (jmolViewer == null) {
			System.out.println("IFSDefaultStructurePropertyManager initializing Jmol...");
			jmolViewer = (Viewer) JmolViewer.allocateViewer(null, null);
		}
		return jmolViewer;
	}

	@Override
	public String getDatasetType(String refName) {
		return getType(refName, null);
	}

}