package com.integratedgraphics.ifs;

import java.io.File;
import java.io.IOException;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSFindingAid;

/**
 * 
 * A test class to extract metadata and representation objects from the ACS
 * sample set of 13 articles. specifically: 
 * <code>
	acs.joc.0c00770
	acs.orglett.0c00571
	acs.orglett.0c00624
	acs.orglett.0c00755
	acs.orglett.0c00788
	acs.orglett.0c00874
	acs.orglett.0c00967
	acs.orglett.0c01022
	acs.orglett.0c01043
	acs.orglett.0c01153
	acs.orglett.0c01197
	acs.orglett.0c01277
	acs.orglett.0c01297
 </code>
 * 
 * 
 * 
 * @author hansonr
 *
 */
public class ExtractorTest extends Extractor {

	public ExtractorTest(File ifsExtractScriptFile, File targetDir, String sourceDir) throws IOException, IFSException {
		// first create super.objects, a List<String>
		initialize(ifsExtractScriptFile);
		// now actually do the extraction.
		if (sourceDir != null)
			setLocalSourceDir(sourceDir);
		// these are the files we want extracted -- no $ for cdx, as that could be cdxml
		setCachePattern("(?<param>procs$|proc2s$|acqus$|acqu2s$)|"
				+ "\\.pdf$|\\.png$|"
				+ "\\.mol$|\\.sdf$|\\.cdx|"
//				+ "\\.log$|\\.out$|\\.txt$|"// maybe put these into JSON only? 
				+ "\\.jdf$|\\.mnova$");
		// this is the pattern to the files we want rezipped. 
		// the <path> group is the one used and should point to the directory just above pdata.
		setRezipCachePattern("^(?<path>.+(?:/|\\|)(?<dir>[^/]+)(?:/|\\|))pdata/[^/]+/procs$", "\\.mnova$");
		IFSFindingAid aid = extractObjects(targetDir);
		System.out.println("extracted " + manifestCount + " files (" + extractedByteCount + " bytes)"
				+ "; ignored " + ignoredCount + " files (" + ignoredByteCount + " bytes)");
	}


	/**
	 * ACS/FigShare codes   /21947274  
	 * 
	 * for example: https://ndownloader.figshare.com/files/21947274
	 */
	private final static String[] testSet = {
			"acs.joc.0c00770/22567817",  // 0 727 files; zips of bruker dirs + mnovas
			"acs.orglett.0c00571/21975525", // 1 3212 files; zips of bruker zips and HRMS
			"acs.orglett.0c00624/21947274", // 2 1143 files; MANY bruker dirs
			"acs.orglett.0c00755/22150197", // 3 MANY bruker dirs
			"acs.orglett.0c00788/22125318", // 4 jdfs
			"acs.orglett.0c00874/22233351", // 5 bruker dirs
			"acs.orglett.0c00967/22111341", // 6 bruker dirs + jdfs
			"acs.orglett.0c01022/22195341", // 7  many mnovas
			"acs.orglett.0c01043/22232721", // 8  single mnova
			"acs.orglett.0c01153/22284726,22284729", // 9 bruker dirs (1 MB)
			"acs.orglett.0c01197/22491647", // 10 many mnovas
			"acs.orglett.0c01277/22613762", // 11 bruker dirs
			"acs.orglett.0c01297/22612484", // 12 bruker dirs
	};
	
	public static void main(String[] args) {

		int i0 = 0;
		int i1 = 0; // 12 max
		
		debugging = false;//true; // verbose listing of all files
		
		int failed = 0;
		
		for (int itest = (args.length == 0 ? i0 : 0); 
				itest <= (args.length == 0 ? i1 : 0); 
				itest++) {
			
//		String s = "test/ok/1c.nmr";
//		Pattern p = Pattern.compile("^\\Qtest/ok/\\E(.+)\\Q.nmr\\E");
//		Matcher m = p.matcher(s);
//		System.out.println(m.find());
//		String v = m.group(1);
//		
//		
			String script, targetDir = null, sourceDir = null;
			switch (args.length) {
			default:
			case 3:
				sourceDir = args[2];
			case 2:
				targetDir = args[1];
			case 1:
				script = args[0];
				break;
			case 0:
				String def = testSet[itest];
				System.out.println("\n!\n! found Test " + itest + " " + def);
				int pt = def.indexOf("/");
				if (pt >= 0)
					def = def.substring(0, pt);
				script = "./extract/" + def + "/IFS-extract.json";
				sourceDir = "file:///c:/temp/iupac/zip";
				targetDir = "c:/temp/iupac/ifs";
			}

			// ./extract/ should be in the main Eclipse project directory.

			try {
				new ExtractorTest(new File(script), new File(targetDir), sourceDir);
			} catch (Exception e) {
				failed++;
				System.err.println("Exception " + e + " for test " + itest);
				e.printStackTrace();
			}

		}
		System.out.println ("DONE failed=" + failed);
	}
	
//	found Test 0 acs.joc.0c00770/22567817
//	2 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22567817}|{id=IFS.structure.param.compound.id::*}.zip|{IFS.nmr.representation.vender.dataset::{id}_{IFS.nmr.param.expt::*}/}}
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22567817}|{id=IFS.structure.param.compound.id::*}.zip|{IFS.nmr.representation.vender.dataset::{id}_mnova}}
//	found 727 files
//	found Test 1 acs.orglett.0c00571/21975525
//	3 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/21975525}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}}
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/21975525}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}}
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/21975525}|FID for Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/HRMS.zip|**/{IFS.ms.representation.pdf::*.pdf}}
//	found 3212 files
//	found Test 2 acs.orglett.0c00624/21947274
//	1 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/21947274}|primary NMR data/{id=IFS.structure.param.compound.id::*}/{IFS.nmr.representation.vender.dataset::{id}-{IFS.nmr.param.expt::*}/}}
//	found 1143 files
//	found Test 3 acs.orglett.0c00755/22150197
//	1 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22150197}|NMR/{id=IFS.structure.param.compound.id::*}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}/}}
//	found 2150 files
//	found Test 4 acs.orglett.0c00788/22125318
//	1 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22125318}|primary NMR data/{IFS.nmr.representation.vender.dataset::{IFS.structure.param.compound.id::*}-{IFS.nmr.param.expt::*}.jdf}}
//	found 82 files
//	found Test 5 acs.orglett.0c00874/22233351
//	1 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22233351}|NMR FID files/{id=IFS.structure.param.compound.id::*}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}/}*}
//	found 1598 files
//	found Test 6 acs.orglett.0c00967/22111341
//	2 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22111341}|{id=IFS.structure.param.compound.id::*}//{IFS.nmr.representation.vender.dataset::{id}-{IFS.nmr.param.expt::*}/}}
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22111341}|{id=IFS.structure.param.compound.id::*}/{IFS.nmr.representation.vender.dataset::{id}-{IFS.nmr.param.expt::*}.jdf}}
//	found 1354 files
//	found Test 7 acs.orglett.0c01022/22195341
//	2 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22195341}|NMR DATA/product/{IFS.nmr.representation.vender.dataset::{id=IFS.structure.param.compound.id::*}-{IFS.nmr.param.expt::*}.mnova}}
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22195341}|NMR DATA/starting material/{IFS.nmr.representation.vender.dataset::{id=IFS.structure.param.compound.id::*}-{IFS.nmr.param.expt::*}.mnova}}
//	found 66 files
//	found Test 8 acs.orglett.0c01043/22232721
//	1 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22232721}|metadataNMR/{IFS.nmr.representation.vender.dataset::NMR spectra.mnova}}
//	found 1 files
//	found Test 9 acs.orglett.0c01153/22284726,22284729
//	4 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22284726}|*.zip|{id=IFS.structure.param.compound.id::*}/|{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}/}}
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22284726}|*.zip|{id=IFS.structure.param.compound.id::*}/|{IFS.structure.representation.cdx::*.cdx}}
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22284729}|*.zip|{id=IFS.structure.param.compound.id::*}/|{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}/}}
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22284729}|*.zip|{id=IFS.structure.param.compound.id::*}/|{IFS.structure.representation.cdx::*.cdx}}
//	found 3476 files
//	found Test 10 acs.orglett.0c01197/22491647
//	2 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22491647}|NMR DATA/products/{IFS.nmr.representation.vender.dataset::{id=IFS.structure.param.compound.id::*}.mnova}}
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22491647}|NMR DATA/Substrate/{IFS.nmr.representation.vender.dataset::{id=IFS.structure.param.compound.id::*}.mnova}}
//	found 61 files
//	found Test 11 acs.orglett.0c01277/22613762
//	1 objects found
//	found object {IFS.finding.aid.object::{IFS.nmr.representation.vender.dataset::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22613762}|alkylation original FID submitted/{id=IFS.structure.param.compound.id::*}-{IFS.nmr.param.expt::*}/**/*}}
//	found 5372 files
//	found Test 12 acs.orglett.0c01297/22612484
//	1 objects found
//	found object {IFS.finding.aid.object::{IFS.finding.aid.source.data.uri::https://ndownloader.figshare.com/files/22612484}|NMR fids/{IFS.nmr.representation.vender.dataset::{id=IFS.structure.param.compound.id::*}/{IFS.nmr.param.expt::*}/}}
//	found 1495 files

	
//FigShare searching:
//import requests as rq
//from pprint import pprint as pp
//
//HEADERS = {'content-type': 'application/json'}
//
//r = rq.post('https://api.figshare.com/v2/articles/search', data='{"search_for": "university of sheffield", "page_size":20}', headers=HEADERS)
//print(r.status_code)
//results = r.json()
//pp(results)
	
}