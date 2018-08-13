package sonchain.blockchain.core;

public enum LinkType {

	None(0),     ///< Unknown link
    Goto(1),     ///< A "Go To" link
    Execute(2),  ///< A command to be executed
    Browse(3),   ///< An URL to be browsed (eg "http://poppler.freedesktop.org")
    Action(4),   ///< A "standard" action to be executed in the viewer
    Sound(5),    ///< A link representing a sound to be played
    Movie(6),    ///< An action to be executed on a movie
    JavaScript(7);    ///< A JavaScript code to be interpreted \since 0.10
    
	private int m_value = 0 ;
	
	private LinkType(int value) {
        this.m_value = value;
    }

    public int Value() {
        return m_value;
    }
	
    @Override
    public String toString() {
    	if(m_value == 0){
    		return "None";
    	}
    	else if(m_value == 1){
    		return "Goto";
    	}
    	else if(m_value == 2){
    		return "Execute";
    	}
    	else if(m_value == 3){
    		return "Browse";
    	}
    	else if(m_value == 4){
    		return "Action";
    	}
    	else if(m_value == 5){
    		return "Sound";
    	}
    	else if(m_value == 6){
    		return "Movie";
    	}
    	else if(m_value == 7){
    		return "JavaScript";
    	}
    	return "";
    }
    
    public static LinkType fromString(String str){
    	if(str.equals("Goto")){
    		return LinkType.Goto;
    	}
    	else if(str.equals("Execute")){
    		return LinkType.Execute;
    	}
    	else if(str.equals("Browse")){
    		return LinkType.Browse;
    	}
    	else if(str.equals("Action")){
    		return LinkType.Action;
    	}
    	else if(str.equals("Sound")){
    		return LinkType.Sound;
    	}
    	else if(str.equals("Movie")){
    		return LinkType.Movie;
    	}
    	else if(str.equals("JavaScript")){
    		return LinkType.JavaScript;
    	}
		return LinkType.None;
    }
}
