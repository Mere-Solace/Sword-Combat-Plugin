package btm.sword.system.inventory;

import java.util.ArrayList;

public class PaginatedMenu {
	private final ArrayList<Menu> pages = new ArrayList<>();
	
	public void addMenuPage(Menu page) {
		pages.add(page);
	}
	
	public void addMenuPage(int index, Menu page) {
		pages.add(index, page);
	}
	
	public void removeMenuPage(Menu page) {
		pages.remove(page);
	}
	
	public void removeLast() {
		pages.removeLast();
	}
	
	public ArrayList<Menu> getPages() {
		return pages;
	}
	
	public Menu getFirst() {
		return pages.getFirst();
	}
	
	public Menu getLast() {
		return pages.getLast();
	}
	
	public Menu getPage(Menu page) {
		return pages.get(pages.indexOf(page));
	}
	
	public Menu getPage(int index) {
		return pages.get(index);
	}
}
