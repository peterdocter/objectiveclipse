/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/ 

package org.eclipse.cdt.internal.ui.typehierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.IDeclaration;
import org.eclipse.cdt.core.model.IMember;
import org.eclipse.cdt.core.model.IMethodDeclaration;
import org.eclipse.cdt.core.parser.ast.ASTAccessVisibility;
import org.eclipse.cdt.refactoring.actions.CRefactoringActionGroup;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.actions.OpenViewActionGroup;

import org.eclipse.cdt.internal.ui.CPluginImages;
import org.eclipse.cdt.internal.ui.IContextMenuConstants;
import org.eclipse.cdt.internal.ui.search.actions.SelectionSearchGroup;
import org.eclipse.cdt.internal.ui.viewsupport.AdaptingSelectionProvider;
import org.eclipse.cdt.internal.ui.viewsupport.CElementImageProvider;
import org.eclipse.cdt.internal.ui.viewsupport.CElementLabels;
import org.eclipse.cdt.internal.ui.viewsupport.CUILabelProvider;
import org.eclipse.cdt.internal.ui.viewsupport.EditorOpener;
import org.eclipse.cdt.internal.ui.viewsupport.SelectionProviderMediator;

/**
 * The view part for the include browser.
 */
public class THViewPart extends ViewPart {
	private static final int MAX_HISTORY_SIZE = 10;
    private static final String TRUE = String.valueOf(true);
//    private static final String KEY_WORKING_SET_FILTER = "workingSetFilter"; //$NON-NLS-1$
    private static final String KEY_SHOW_FILES= "showFilesInLabels"; //$NON-NLS-1$
    private static final String KEY_SHOW_INHERITED_MEMBERS= "showInheritedMembers"; //$NON-NLS-1$
    private static final String KEY_FILTER_FIELDS= "filterFields"; //$NON-NLS-1$
    private static final String KEY_FILTER_STATIC= "filterStatic"; //$NON-NLS-1$
    private static final String KEY_FILTER_NON_PUBLIC= "filterNonPublic"; //$NON-NLS-1$
    private static final String KEY_MODE= "hierarchyMode"; //$NON-NLS-1$
    private static final String KEY_ORIENTATION= "viewOrientation"; //$NON-NLS-1$
	private static final String KEY_SPLITTER_W1 = "splitterWeight1"; //$NON-NLS-1$
	private static final String KEY_SPLITTER_W2 = "splitterWeight2"; //$NON-NLS-1$

	// constants for view orientation
	private static final int ORIENTATION_AUTOMATIC = 0;
	private static final int ORIENTATION_HORIZONTAL = 1;
	private static final int ORIENTATION_VERTICAL = 2;
	private static final int ORIENTATION_SINGLE = 3;
	private static final int METHOD_LABEL_OPTIONS_SIMPLE = CElementLabels.M_PARAMETER_TYPES;
	private static final int METHOD_LABEL_OPTIONS_QUALIFIED = METHOD_LABEL_OPTIONS_SIMPLE | CElementLabels.ALL_POST_QUALIFIED;
	private static final int METHOD_ICON_OPTIONS = CElementImageProvider.OVERLAY_ICONS;
    
    private IMemento fMemento;
    private boolean fShowsMessage= true;
	private int fCurrentViewOrientation= -1;
	private boolean fInComputeOrientation= false;

	private ArrayList fHistoryEntries= new ArrayList(MAX_HISTORY_SIZE);

    // widgets
    private PageBook fPagebook;
    private Composite fInfoPage;
    private Text fInfoText;
	private SashForm fSplitter;
	private ViewForm fHierarchyViewForm;
	private ViewForm fMethodViewForm;
	private CLabel fMethodLabel;

    // viewers
	private THHierarchyModel fModel;
	private THLabelProvider fHierarchyLabelProvider;
	private CUILabelProvider fMethodLabelProvider;
	private TableViewer fMethodViewer;
	private TreeViewer fHierarchyTreeViewer;

    // filters, sorter
//	private WorkingSetFilterUI fWorkingSetFilterUI;

    // actions
	private ToolBarManager fMethodToolbarManager;
    private Action fShowSuperTypeHierarchyAction;
    private Action fShowSubTypeHierarchyAction;
    private Action fShowTypeHierarchyAction;
    private Action fShowInheritedMembersAction;
    private Action fShowFilesInLabelsAction;
    private Action fRefreshAction;
    private Action fCancelAction;
	private Action fHistoryAction;
	private Action fOpenElement;
	private Action fHorizontalOrientation;
	private Action fVerticalOrientation;
	private Action fAutomaticOrientation;
	private Action fSingleOrientation;

	private Action fFieldFilterAction;
	private Action fStaticFilterAction;
	private Action fNonPublicFilterAction;

	private ViewerFilter fFieldFilter;
	private ViewerFilter fStaticFilter;
	private ViewerFilter fNonPublicFilter;

	// action groups
	private OpenViewActionGroup fOpenViewActionGroup;
	private SelectionSearchGroup fSelectionSearchGroup;
	private CRefactoringActionGroup fRefactoringActionGroup;
	private int fIgnoreSelectionChanges= 0;

    
    public void setFocus() {
        fPagebook.setFocus();
    }

    public void setMessage(String msg) {
        fInfoText.setText(msg);
        fPagebook.showPage(fInfoPage);
        fShowsMessage= true;
        updateDescription();
        updateActionEnablement();
    }
    
    void setInput(ICElement input) {
    	if (input == null) {
            setMessage(Messages.THViewPart_instruction);
            fHierarchyTreeViewer.setInput(null);
            fMethodViewer.setInput(null);
            return;
    	}
        fShowsMessage= false;
        fModel.setInput(input);
        fHierarchyTreeViewer.setInput(fModel);
        fMethodViewer.setInput(fModel);
        fPagebook.showPage(fSplitter);
        updateDescription();
    	updateHistory(input);
    	updateActionEnablement();
    	fModel.computeGraph();
    }

	public void createPartControl(Composite parent) {
        fPagebook = new PageBook(parent, SWT.NULL);
        fPagebook.setLayoutData(new GridData(GridData.FILL_BOTH));
        createInfoPage();
        createViewerPage();
                
        initSelectionProvider();

        initDragAndDrop();
        createActions();
        createContextMenu();

        setMessage(Messages.THViewPart_instruction);
        initializeActionStates();
    }
	
	private void initSelectionProvider() {
		SelectionProviderMediator mediator= new SelectionProviderMediator();
		mediator.addViewer(fHierarchyTreeViewer);
		mediator.addViewer(fMethodViewer);
		getSite().setSelectionProvider(new AdaptingSelectionProvider(ICElement.class, mediator));
	}

	public void dispose() {
		if (fOpenViewActionGroup != null) {
			fOpenViewActionGroup.dispose();
			fOpenViewActionGroup= null;
		}
		if (fSelectionSearchGroup != null) {
			fSelectionSearchGroup.dispose();
			fSelectionSearchGroup= null;
		}
		if (fRefactoringActionGroup != null) {
			fRefactoringActionGroup.dispose();
			fRefactoringActionGroup= null;
		}
//		if (fWorkingSetFilterUI != null) {
//			fWorkingSetFilterUI.dispose();
//			fWorkingSetFilterUI= null;
//		}
		super.dispose();
	}
	
    private void initializeActionStates() {
        int mode= THHierarchyModel.TYPE_HIERARCHY;
        int orientation= ORIENTATION_AUTOMATIC;
        boolean showFiles= false;
        boolean showInheritedMembers= false;
        boolean hideFields= false;
        boolean hideStatic= false;
        boolean hideNonPublic= false;
        int[] weights= {35,65};
        
        if (fMemento != null) {
            showFiles= TRUE.equals(fMemento.getString(KEY_SHOW_FILES));
            showInheritedMembers= TRUE.equals(fMemento.getString(KEY_SHOW_INHERITED_MEMBERS));
            hideFields= TRUE.equals(fMemento.getString(KEY_FILTER_FIELDS));
            hideStatic= TRUE.equals(fMemento.getString(KEY_FILTER_STATIC));
            hideNonPublic= TRUE.equals(fMemento.getString(KEY_FILTER_NON_PUBLIC));
            Integer intval= fMemento.getInteger(KEY_MODE);
            if (intval != null) {
            	mode= intval.intValue();
            }
            intval= fMemento.getInteger(KEY_ORIENTATION);
            if (intval != null) {
            	orientation= intval.intValue();
            }
            intval= fMemento.getInteger(KEY_SPLITTER_W1);
            Integer intval2= fMemento.getInteger(KEY_SPLITTER_W2);
            if (intval != null && intval2 != null) {
            	weights[0]= intval.intValue();
            	weights[1]= intval2.intValue();
            }
        }
        restoreOrientation(orientation);
        restoreHierarchyKind(mode);
		fSplitter.setWeights(weights);

		fShowInheritedMembersAction.setChecked(showInheritedMembers);
		fShowInheritedMembersAction.run();
		
		fFieldFilterAction.setChecked(hideFields);
		fFieldFilterAction.run();
		fStaticFilterAction.setChecked(hideStatic);
		fStaticFilterAction.run();
		fNonPublicFilterAction.setChecked(hideNonPublic);
		fNonPublicFilterAction.run();

		fHierarchyLabelProvider.setShowFiles(showFiles);
        fShowFilesInLabelsAction.setChecked(showFiles);
  
		fMethodToolbarManager.update(true);
    }

	public void init(IViewSite site, IMemento memento) throws PartInitException {
        fMemento= memento;
        super.init(site, memento);
    }


    public void saveState(IMemento memento) {
//        if (fWorkingSetFilterUI != null) {
//        	fWorkingSetFilterUI.saveState(memento, KEY_WORKING_SET_FILTER);
//        }
    	memento.putString(KEY_SHOW_INHERITED_MEMBERS, String.valueOf(fShowInheritedMembersAction.isChecked()));
        memento.putString(KEY_SHOW_FILES, String.valueOf(fShowFilesInLabelsAction.isChecked()));
        memento.putString(KEY_FILTER_FIELDS, String.valueOf(fFieldFilterAction.isChecked()));
        memento.putString(KEY_FILTER_STATIC, String.valueOf(fStaticFilterAction.isChecked()));
        memento.putString(KEY_FILTER_NON_PUBLIC, String.valueOf(fNonPublicFilterAction.isChecked()));
		int[] weights= fSplitter.getWeights();
        memento.putInteger(KEY_SPLITTER_W1, weights[0]);
        memento.putInteger(KEY_SPLITTER_W2, weights[1]);
        if (fAutomaticOrientation.isChecked()) {
        	memento.putInteger(KEY_ORIENTATION, ORIENTATION_AUTOMATIC);
        }
        else {
        	memento.putInteger(KEY_ORIENTATION, fCurrentViewOrientation);
        }
        super.saveState(memento);
    }

    private void createContextMenu() {
        IWorkbenchPartSite site = getSite();
    	
        // hierarchy
    	MenuManager manager = new MenuManager();
    	manager.setRemoveAllWhenShown(true);
    	manager.addMenuListener(new IMenuListener() {
    		public void menuAboutToShow(IMenuManager m) {
    			onContextMenuAboutToShow(m, true);
    		}
    	});
    	Menu menu = manager.createContextMenu(fHierarchyTreeViewer.getControl());
    	fHierarchyTreeViewer.getControl().setMenu(menu);
    	site.registerContextMenu(CUIPlugin.ID_TYPE_HIERARCHY, manager, fHierarchyTreeViewer); 

    	
    	manager = new MenuManager();
    	manager.setRemoveAllWhenShown(true);
    	manager.addMenuListener(new IMenuListener() {
    		public void menuAboutToShow(IMenuManager m) {
    			onContextMenuAboutToShow(m, false);
    		}
    	});
    	menu = manager.createContextMenu(fMethodViewer.getControl());
    	fMethodViewer.getControl().setMenu(menu);
    	site.registerContextMenu(CUIPlugin.ID_TYPE_HIERARCHY + ".methods", manager, fMethodViewer); //$NON-NLS-1$
    }

	protected void onContextMenuAboutToShow(IMenuManager menu, boolean hierarchyView) {
		CUIPlugin.createStandardGroups(menu);
		StructuredViewer viewer= hierarchyView ? (StructuredViewer) fHierarchyTreeViewer : fMethodViewer;
		final ICElement elem= selectionToElement(viewer.getSelection());
		if (elem != null) {
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpenElement);
			if (hierarchyView && !elem.equals(fModel.getInput())) {
				String label= MessageFormat.format(Messages.THViewPart_FocusOn, 
						new Object[] {
							CElementLabels.getTextLabel(elem, CElementLabels.ALL_FULLY_QUALIFIED | CElementLabels.M_PARAMETER_TYPES)
				});
				menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, new Action(label) {
					public void run() {
						setInput(elem);
					}
				});
			}
		}
		
		// action groups
		ISelection selection = getSite().getSelectionProvider().getSelection();
		if (OpenViewActionGroup.canActionBeAdded(selection)){
			fOpenViewActionGroup.fillContextMenu(menu);
		}

		if (SelectionSearchGroup.canActionBeAdded(selection)){
			fSelectionSearchGroup.fillContextMenu(menu);
		}
		fRefactoringActionGroup.fillContextMenu(menu);
	}

	private void createViewerPage() {
		fSplitter= new SashForm(fPagebook, SWT.VERTICAL);
		fSplitter.setLayoutData(new GridData(GridData.FILL_BOTH));
		fSplitter.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {
			}
			public void controlResized(ControlEvent e) {
				if (fAutomaticOrientation.isChecked()) {
					setOrientation(ORIENTATION_AUTOMATIC);
				}
			}
		});

		fHierarchyViewForm= new ViewForm(fSplitter, SWT.NONE);
		Control hierarchyControl= createHierarchyControl(fHierarchyViewForm);
		fHierarchyViewForm.setContent(hierarchyControl);
				
		fMethodViewForm= new ViewForm(fSplitter, SWT.NONE);
		Control methodControl= createMethodControl(fMethodViewForm);
		fMethodViewForm.setContent(methodControl);
		
		fMethodLabel = new CLabel(fMethodViewForm, SWT.NONE);
    	fMethodLabel.setText(Messages.THViewPart_MethodPane_title); 
		fMethodViewForm.setTopLeft(fMethodLabel);
	}
   
	private Control createMethodControl(ViewForm parent) {
		fMethodLabelProvider= new CUILabelProvider(METHOD_LABEL_OPTIONS_SIMPLE, METHOD_ICON_OPTIONS);
		fMethodViewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		fMethodViewer.setContentProvider(new THMethodContentProvider());
		fMethodViewer.setLabelProvider(fMethodLabelProvider);
		fMethodViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				onOpenElement(event.getSelection());
			}
		});
    	fMethodViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onMethodSelectionChanged(event);
			}
    	});
    	fMethodViewer.setSorter(new ViewerSorter() {
    		public int category(Object element) {
    			if (element instanceof ICElement) {
    				ICElement celem= (ICElement)element;
    				switch (celem.getElementType()) {
    					case ICElement.C_FIELD: return 1;
    					case ICElement.C_METHOD: 
    					case ICElement.C_METHOD_DECLARATION:
    						IMethodDeclaration md= (IMethodDeclaration) celem;
    						try {
								if (md.isConstructor()) return 2;
								if (md.isDestructor()) return 3;
							} catch (CModelException e) {
								CUIPlugin.getDefault().log(e);
							}
    						break;
    				}
    			}
    			return 10;
    		}
    	});   
        
		ToolBar methodToolBar= new ToolBar(parent, SWT.FLAT | SWT.WRAP);
		parent.setTopCenter(methodToolBar);
		fMethodToolbarManager= new ToolBarManager(methodToolBar);
    	return fMethodViewer.getControl();
	}

	protected void onMethodSelectionChanged(SelectionChangedEvent event) {
		if (fIgnoreSelectionChanges == 0) {
		}
	}

	private Control createHierarchyControl(ViewForm parent) {
		Display display= getSite().getShell().getDisplay();
		fModel= new THHierarchyModel(this, display);
		fHierarchyLabelProvider= new THLabelProvider(display, fModel);
    	fHierarchyTreeViewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    	fHierarchyTreeViewer.setContentProvider(new THContentProvider());
    	fHierarchyTreeViewer.setLabelProvider(fHierarchyLabelProvider);
    	fHierarchyTreeViewer.setSorter(new ViewerSorter());
    	fHierarchyTreeViewer.setUseHashlookup(true);
    	fHierarchyTreeViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				onOpenElement(event.getSelection());
			}
		});

    	fHierarchyTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onHierarchySelectionChanged(event);
			}
    	});
    	    	    
    	return fHierarchyTreeViewer.getControl();
   	}	

    protected void onHierarchySelectionChanged(SelectionChangedEvent event) {
		if (fIgnoreSelectionChanges == 0) {
			THNode node= selectionToNode(event.getSelection());
			fModel.onHierarchySelectionChanged(node);
			fMethodViewer.refresh();
			updateDescription();
		}
	}

	private void createInfoPage() {
        fInfoPage = new Composite(fPagebook, SWT.NULL);
        fInfoPage.setLayoutData(new GridData(GridData.FILL_BOTH));
        fInfoPage.setSize(100, 100);
        fInfoPage.setLayout(new FillLayout());

        fInfoText= new Text(fInfoPage, SWT.WRAP | SWT.READ_ONLY); 
    }

    private void initDragAndDrop() {
        THDropTargetListener dropListener= new THDropTargetListener(this);
        Transfer[] localSelectionTransfer= new Transfer[] {
        		LocalSelectionTransfer.getTransfer()
        };
        DropTarget dropTarget = new DropTarget(fPagebook, DND.DROP_COPY);
        dropTarget.setTransfer(localSelectionTransfer);
        dropTarget.addDropListener(dropListener);
    }

    private void createActions() {
    	// action gruops
    	fOpenViewActionGroup= new OpenViewActionGroup(this);
    	fSelectionSearchGroup= new SelectionSearchGroup(getSite());
    	fRefactoringActionGroup= new CRefactoringActionGroup(this);
    	
//    	fWorkingSetFilterUI= new WorkingSetFilterUI(this, fMemento, KEY_WORKING_SET_FILTER) {
//            protected void onWorkingSetChange() {
//                updateWorkingSetFilter(this);
//            }
//            protected void onWorkingSetNameChange() {
//                updateDescription();
//            }
//        };

		fHorizontalOrientation= new Action(Messages.THViewPart_HorizontalOrientation, IAction.AS_RADIO_BUTTON) {
			public void run() {
				setOrientation(ORIENTATION_HORIZONTAL);
			}
		};
		CPluginImages.setImageDescriptors(fHorizontalOrientation, CPluginImages.T_LCL, CPluginImages.IMG_LCL_HORIZONTAL_ORIENTATION);

		fVerticalOrientation= new Action(Messages.THViewPart_VerticalOrientation, IAction.AS_RADIO_BUTTON) {
			public void run() {
				setOrientation(ORIENTATION_VERTICAL);
			}
		};
		CPluginImages.setImageDescriptors(fVerticalOrientation, CPluginImages.T_LCL, CPluginImages.IMG_LCL_VERTICAL_ORIENTATION);

		fAutomaticOrientation= new Action(Messages.THViewPart_AutomaticOrientation, IAction.AS_RADIO_BUTTON) {
			public void run() {
				setOrientation(ORIENTATION_AUTOMATIC);
			}
		};
		CPluginImages.setImageDescriptors(fAutomaticOrientation, CPluginImages.T_LCL, CPluginImages.IMG_LCL_AUTOMATIC_ORIENTATION);

		fSingleOrientation= new Action(Messages.THViewPart_SinglePaneOrientation, IAction.AS_RADIO_BUTTON) {
			public void run() {
				setOrientation(ORIENTATION_SINGLE);
			}
		};
		CPluginImages.setImageDescriptors(fSingleOrientation, CPluginImages.T_LCL, CPluginImages.IMG_LCL_SINGLE_ORIENTATION);

		fShowTypeHierarchyAction= new Action(Messages.THViewPart_CompleteTypeHierarchy, IAction.AS_RADIO_BUTTON) {
			public void run() {
				if (isChecked()) {
					onSetHierarchyKind(THHierarchyModel.TYPE_HIERARCHY);
				}
			}
        };
        fShowTypeHierarchyAction.setToolTipText(Messages.THViewPart_CompleteTypeHierarchy_tooltip);
        CPluginImages.setImageDescriptors(fShowTypeHierarchyAction, CPluginImages.T_LCL, CPluginImages.IMG_LCL_TYPE_HIERARCHY);       

		fShowSubTypeHierarchyAction= new Action(Messages.THViewPart_SubtypeHierarchy, IAction.AS_RADIO_BUTTON) {
			public void run() {
				if (isChecked()) {
					onSetHierarchyKind(THHierarchyModel.SUB_TYPE_HIERARCHY);
				}
			}
        };
        fShowSubTypeHierarchyAction.setToolTipText(Messages.THViewPart_SubtypeHierarchy_tooltip);
        CPluginImages.setImageDescriptors(fShowSubTypeHierarchyAction, CPluginImages.T_LCL, CPluginImages.IMG_LCL_SUB_TYPE_HIERARCHY);       

		fShowSuperTypeHierarchyAction= new Action(Messages.THViewPart_SupertypeHierarchy, IAction.AS_RADIO_BUTTON) {
			public void run() {
				if (isChecked()) {
					onSetHierarchyKind(THHierarchyModel.SUPER_TYPE_HIERARCHY);
				}
			}
        };
        fShowSuperTypeHierarchyAction.setToolTipText(Messages.THViewPart_SupertypeHierarchy_tooltip);
        CPluginImages.setImageDescriptors(fShowSuperTypeHierarchyAction, CPluginImages.T_LCL, CPluginImages.IMG_LCL_SUPER_TYPE_HIERARCHY);       

		fShowInheritedMembersAction= new Action(Messages.THViewPart_ShowInherited_label, IAction.AS_CHECK_BOX) {
			public void run() {
				onShowInheritedMembers(isChecked());
			}
        };
        fShowInheritedMembersAction.setToolTipText(Messages.THViewPart_ShowInherited_tooltip);
        CPluginImages.setImageDescriptors(fShowInheritedMembersAction, CPluginImages.T_LCL, CPluginImages.IMG_LCL_SHOW_INHERITED_MEMBERS);       

        fFieldFilter= new ViewerFilter() {
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof ICElement) {
                	ICElement node= (ICElement) element;
                	switch (node.getElementType()) {
                	case ICElement.C_ENUMERATOR:
                	case ICElement.C_FIELD:
                	case ICElement.C_TEMPLATE_VARIABLE:
                	case ICElement.C_VARIABLE:
                	case ICElement.C_VARIABLE_DECLARATION:
                	case ICElement.C_VARIABLE_LOCAL:
                		return false;
                	}
                }
                return true;
            }
        };
        fStaticFilter= new ViewerFilter() {
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof IDeclaration) {
                	IDeclaration node= (IDeclaration) element;
                	try {
						return !node.isStatic();
					} catch (CModelException e) {
						CUIPlugin.getDefault().log(e);
					}
                }
                return true;
            }
        };
        fNonPublicFilter= new ViewerFilter() {
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof IMember) {
                	IMember node= (IMember) element;
                	try {
						return ASTAccessVisibility.PUBLIC.equals(node.getVisibility());
					} catch (CModelException e) {
						CUIPlugin.getDefault().log(e);
					}
                }
                return true;
            }
        };
        fFieldFilterAction= new Action(Messages.THViewPart_HideFields_label, IAction.AS_CHECK_BOX) {
            public void run() {
                if (isChecked()) {
                    fMethodViewer.addFilter(fFieldFilter);
                }
                else {
                	fMethodViewer.removeFilter(fFieldFilter);
                }
            }
        };
        fFieldFilterAction.setToolTipText(Messages.THViewPart_HideFields_tooltip);
        CPluginImages.setImageDescriptors(fFieldFilterAction, CPluginImages.T_LCL, CPluginImages.IMG_ACTION_HIDE_FIELDS);       

        fStaticFilterAction= new Action(Messages.THViewPart_HideStatic_label, IAction.AS_CHECK_BOX) {
            public void run() {
                if (isChecked()) {
                    fMethodViewer.addFilter(fStaticFilter);
                }
                else {
                	fMethodViewer.removeFilter(fStaticFilter);
                }
            }
        };
        fStaticFilterAction.setToolTipText(Messages.THViewPart_HideStatic_tooltip);
        CPluginImages.setImageDescriptors(fStaticFilterAction, CPluginImages.T_LCL, CPluginImages.IMG_ACTION_HIDE_STATIC);       

        fNonPublicFilterAction= new Action(Messages.THViewPart_HideNonPublic_label, IAction.AS_CHECK_BOX) {
            public void run() {
                if (isChecked()) {
                    fMethodViewer.addFilter(fNonPublicFilter);
                }
                else {
                	fMethodViewer.removeFilter(fNonPublicFilter);
                }
            }
        };
        fNonPublicFilterAction.setToolTipText(Messages.THViewPart_HideNonPublic_tooltip);
        CPluginImages.setImageDescriptors(fNonPublicFilterAction, CPluginImages.T_LCL, CPluginImages.IMG_ACTION_SHOW_PUBLIC);       

        fOpenElement= new Action(Messages.THViewPart_Open) {
        	public void run() {
        		onOpenElement(getSite().getSelectionProvider().getSelection());
        	}
        };
        fOpenElement.setToolTipText(Messages.THViewPart_Open_tooltip);
        
        fShowFilesInLabelsAction= new Action(Messages.THViewPart_ShowFileNames, IAction.AS_CHECK_BOX) {
            public void run() {
                onShowFilesInLabels(isChecked());
            }
        };
        fShowFilesInLabelsAction.setToolTipText(Messages.THViewPart_ShowFileNames_tooltip);

        fRefreshAction = new Action(Messages.THViewPart_Refresh) {
            public void run() {
                onRefresh();
            }
        };
        fRefreshAction.setToolTipText(Messages.THViewPart_Refresh_tooltip); 
        CPluginImages.setImageDescriptors(fRefreshAction, CPluginImages.T_LCL, CPluginImages.IMG_REFRESH);       

        fCancelAction = new Action(Messages.THViewPart_Cancel) {
            public void run() {
                onCancel();
            }
        };
        fCancelAction.setToolTipText(Messages.THViewPart_Cancel_tooltip); 
        CPluginImages.setImageDescriptors(fCancelAction, CPluginImages.T_LCL, CPluginImages.IMG_LCL_CANCEL);       

        fHistoryAction = new THHistoryDropDownAction(this);

        // setup action bar
        // global action hooks
        IActionBars actionBars = getViewSite().getActionBars();
        fRefactoringActionGroup.fillActionBars(actionBars);
        fOpenViewActionGroup.fillActionBars(actionBars);
        fSelectionSearchGroup.fillActionBars(actionBars);
        
        actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), fRefreshAction);
        actionBars.updateActionBars();
        
        // local toolbar
        IToolBarManager tm = actionBars.getToolBarManager();
        tm.add(fShowTypeHierarchyAction);
        tm.add(fShowSuperTypeHierarchyAction);
        tm.add(fShowSubTypeHierarchyAction);
		tm.add(fHistoryAction);
        tm.add(fRefreshAction);
        tm.add(fCancelAction);

        // local menu
        IMenuManager mm = actionBars.getMenuManager();

//        fWorkingSetFilterUI.fillActionBars(actionBars);
//        mm.add(new Separator(IContextMenuConstants.GROUP_SHOW));
        mm.add(fShowTypeHierarchyAction);
        mm.add(fShowSuperTypeHierarchyAction);
        mm.add(fShowSubTypeHierarchyAction);
        mm.add(new Separator(IContextMenuConstants.GROUP_VIEWER_SETUP));

        MenuManager submenu= new MenuManager(Messages.THViewPart_LayoutMenu);
		submenu.add(fHorizontalOrientation);
		submenu.add(fVerticalOrientation);
		submenu.add(fAutomaticOrientation);
		submenu.add(fSingleOrientation);

		mm.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, submenu);
        mm.add(new Separator());
        mm.add(fShowFilesInLabelsAction);
        
        // method toolbar
        fMethodToolbarManager.add(fShowInheritedMembersAction);
        fMethodToolbarManager.add(new Separator());
        fMethodToolbarManager.add(fFieldFilterAction);
        fMethodToolbarManager.add(fStaticFilterAction);
        fMethodToolbarManager.add(fNonPublicFilterAction);        
    }
            
	protected void onOpenElement(ISelection selection) {
    	ICElement elem= selectionToElement(selection);
    	openElement(elem);
	}

	private void openElement(ICElement elem) {
		if (elem != null) {
			IWorkbenchPage page= getSite().getPage();
			try {
				EditorOpener.open(page, elem);
			} catch (CModelException e) {
				CUIPlugin.getDefault().log(e);
			}
		}
	}

    protected void onRefresh() {
    	fModel.refresh();
    	updateActionEnablement();
    }

    protected void onCancel() {
    	fModel.stopGraphComputation();
    	updateView();
    }

    protected void onShowFilesInLabels(boolean show) {
    	fHierarchyLabelProvider.setShowFiles(show);
    	fHierarchyTreeViewer.refresh();
    }

    private void updateHistory(ICElement input) {
    	if (input != null) {
    		fHistoryEntries.remove(input);
    		fHistoryEntries.add(0, input);
    		if (fHistoryEntries.size() > MAX_HISTORY_SIZE) {
    			fHistoryEntries.remove(MAX_HISTORY_SIZE-1);
    		}
    	}
	}
    
    private void updateDescription() {
        String message= ""; //$NON-NLS-1$
        if (!fShowsMessage) {
        	ICElement elem= getInput();
            if (elem != null) {
                String label;
            	
                // label
                label= CElementLabels.getElementLabel(elem, 0);
            	
                // scope
                IWorkingSet workingSet= null;
//                workingSet= fWorkingSetFilterUI.getWorkingSet();
            	if (workingSet == null) {	
            		message= label;
            	}
            	else {
            		String scope= workingSet.getLabel();
                	message= MessageFormat.format("{0} - {1}", new Object[] {label, scope}); //$NON-NLS-1$
            	}
            	
            	label= ""; //$NON-NLS-1$
            	Image image= null;
            	THNode node= fModel.getSelectionInHierarchy();
            	if (node != null) {
            		elem= node.getRepresentedDeclaration();
            		if (elem != null) {
            			label= CElementLabels.getElementLabel(elem, 0);
            			image= fHierarchyLabelProvider.getImage(elem);
            		}
            	}
            	fMethodLabel.setText(label);
            	fMethodLabel.setImage(image);
            }
        }
        setContentDescription(message);
    }
    
	private void updateActionEnablement() {
		fHistoryAction.setEnabled(!fHistoryEntries.isEmpty());
		fRefreshAction.setEnabled(!fShowsMessage);
		fCancelAction.setEnabled(!fShowsMessage && !fModel.isComputed());
		fShowSubTypeHierarchyAction.setEnabled(!fShowsMessage);
		fShowSuperTypeHierarchyAction.setEnabled(!fShowsMessage);
		fShowTypeHierarchyAction.setEnabled(!fShowsMessage);
	}

//    private void updateWorkingSetFilter(WorkingSetFilterUI filterUI) {
//    	fModel.setWorkingSetFilter(filterUI);
//    	updateView();
//    }
    
    protected void onSetHierarchyKind(int kind) {
    	if (fModel.getHierarchyKind() != kind) {
    		fModel.setHierarchyKind(kind);
    		updateView();
    	}
    }

    protected void onShowInheritedMembers(boolean show) {
    	if (fModel.isShowInheritedMembers() != show) {
    		fModel.setShowInheritedMembers(show);
    		fMethodLabelProvider.setTextFlags(show ? 
    				METHOD_LABEL_OPTIONS_QUALIFIED : METHOD_LABEL_OPTIONS_SIMPLE);
    		fMethodViewer.refresh();
    	}
    }
    
    private void updateView() {
    	if (!fShowsMessage) {
    		fIgnoreSelectionChanges++;
    		try {
    			fHierarchyTreeViewer.refresh();
    			fMethodViewer.refresh();
    			updateDescription();
    			updateActionEnablement();
    			setSelections();
    		}
    		finally {
    			fIgnoreSelectionChanges--;
    		}
    	}
	}

	private void setSelections() {
		fIgnoreSelectionChanges++;
		try {
			THNode node= fModel.getSelectionInHierarchy();
			if (node != null) {
				fHierarchyTreeViewer.setSelection(new StructuredSelection(node));
				fHierarchyTreeViewer.expandToLevel(node, 1);
			}
		}
		finally {
			fIgnoreSelectionChanges--;
		}
	}

	private ICElement selectionToElement(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			for (Iterator iter = ss.iterator(); iter.hasNext(); ) {
				Object cand= iter.next();
				if (cand instanceof ICElement) {
					return (ICElement) cand;
				}
				if (cand instanceof IAdaptable) {
					ICElement elem= (ICElement) ((IAdaptable) cand).getAdapter(ICElement.class);
					if (elem != null) {
						return elem;
					}
				}
			}
		}
		return null;
	}

	private THNode selectionToNode(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			for (Iterator iter = ss.iterator(); iter.hasNext(); ) {
				Object cand= iter.next();
				if (cand instanceof THNode) {
					return (THNode) cand;
				}
			}
		}
		return null;
	}

	public Control getPageBook() {
		return fPagebook;
	}

	public ICElement[] getHistoryEntries() {
		return (ICElement[]) fHistoryEntries.toArray(new ICElement[fHistoryEntries.size()]);
	}

	public void setHistoryEntries(ICElement[] remaining) {
		fHistoryEntries.clear();
		fHistoryEntries.addAll(Arrays.asList(remaining));
	}

	ICElement getInput() {
        Object input= fModel.getInput();
        if (input instanceof ICElement) {
        	return (ICElement) input;
        }
        return null;
	}

	public TreeViewer getHiearchyViewer() {
		return fHierarchyTreeViewer;
	}

	public TableViewer getMethodViewer() {
		return fMethodViewer;
	}
	
	private void restoreOrientation(int orientation) {
		switch(orientation) {
			case ORIENTATION_HORIZONTAL:
				fHorizontalOrientation.setChecked(true);
				break;
			case ORIENTATION_VERTICAL:
				fVerticalOrientation.setChecked(true);
				break;
			case ORIENTATION_SINGLE:
				fSingleOrientation.setChecked(true);
				break;
			default:
				orientation= ORIENTATION_AUTOMATIC;
				fAutomaticOrientation.setChecked(true);
				break;
		}
		setOrientation(orientation);
	}
	
    private void restoreHierarchyKind(int kind) {
		switch(kind) {
		case THHierarchyModel.SUB_TYPE_HIERARCHY:
			fShowSubTypeHierarchyAction.setChecked(true);
			break;
		case THHierarchyModel.SUPER_TYPE_HIERARCHY:
			fShowSuperTypeHierarchyAction.setChecked(true);
			break;
		default:
			kind= THHierarchyModel.TYPE_HIERARCHY;
			fShowTypeHierarchyAction.setChecked(true);
			break;
		}			
		fModel.setHierarchyKind(kind);
	}

	public void setOrientation(int orientation) {
		if (fInComputeOrientation) {
			return;
		}
		fInComputeOrientation= true;
		try {
			if (fCurrentViewOrientation != orientation) {
				if (fSplitter != null && !fSplitter.isDisposed()) {
					if (orientation == ORIENTATION_AUTOMATIC) {
						orientation= getBestOrientation();
					}
					if (orientation == ORIENTATION_SINGLE) {
						fMethodViewForm.setVisible(false);
					} else {
						if (fCurrentViewOrientation == ORIENTATION_SINGLE) {
							fMethodViewForm.setVisible(true);
						}
						boolean horizontal= orientation == ORIENTATION_HORIZONTAL;
						fSplitter.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
					}
					fSplitter.layout();
				}
				fCurrentViewOrientation= orientation;
			}
		} finally {
			fInComputeOrientation= false;
		}
	}

	private int getBestOrientation() {
		Point size= fSplitter.getSize();
		if (size.x != 0 && size.y != 0) {
			if (3*size.x < 2*size.y) 
				return ORIENTATION_VERTICAL;
		}
		return ORIENTATION_HORIZONTAL;
	}

	public void onEvent(int event) {
		switch (event) {
		case THHierarchyModel.END_OF_COMPUTATION:
			updateView();
			break;
		}		
	}
}
