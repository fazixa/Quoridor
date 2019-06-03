/*
 * Created on Aug 17, 2006 
 */
package martijn.quoridor.brains;

import java.util.List;
import ArtificialStupidity.*;
/**
 * The DefaultBrainFactory provides the default brains used in the Quoridor
 * application.
 *
 */
public class DefaultBrainFactory implements BrainFactory {

	public void addBrains(List<Brain> brains) {
		brains.add(new StupidBrain());
		//brains.add(new TestBrain());
	}

}
