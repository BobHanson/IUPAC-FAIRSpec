package org.iupac.fairdata.spec.ms;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDProperty;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.spec.IFDSpecData;
import org.iupac.fairdata.spec.IFDSpecDataRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public final class IFDMSSpecData extends IFDSpecData {


	public static final String IFD_PROP_SPEC_MS_EXPT_LABEL    = "IFD.property.spec.ms.expt.label";


	{
		super.setProperties(new IFDProperty[] {
				new IFDProperty(IFDMSSpecData.IFD_PROP_SPEC_MS_EXPT_LABEL),
		});
	}
	

	public IFDMSSpecData() throws IFDException {
		super(null, "spec.ms");
	}

	@Override
	protected IFDSpecData newInstance() throws IFDException {
		return new IFDMSSpecData();
	}


	@Override
	protected IFDSpecDataRepresentation newRepresentation(String name, IFDReference ref, Object obj, long len, String type, String subtype) {
		return new IFDMSSpecDataRepresentation(ref, obj, len, type, subtype);
	}


}
